import React, { useState, useRef, useEffect, useMemo } from 'react';
import { Send, Mic, Camera, BookOpen, ScanBarcode, X, Image as ImageIcon, Square } from 'lucide-react';
import { useAppContext } from '../context/AppContext';
import RecipeList from './RecipeList';

const ChatInterface = ({ onSend, onOpenBarcode, onClose, initialMode }) => {
    const { logs, chatMessages, setChatMessages } = useAppContext();
    const [input, setInput] = useState('');
    const [isListening, setIsListening] = useState(false);
    const [showRecipes, setShowRecipes] = useState(false);
    const [pendingImage, setPendingImage] = useState(null);

    const fileInputRef = useRef(null);
    const messagesEndRef = useRef(null);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [chatMessages, pendingImage]); // Scroll also when image is attached

    const hasTriggered = useRef(false);

    // Handle initial actions (Camera/Voice shortcuts)
    useEffect(() => {
        if (initialMode === 'camera' && !hasTriggered.current) {
            hasTriggered.current = true;
            // Small timeout to ensure render
            setTimeout(() => fileInputRef.current?.click(), 100);
        } else if (initialMode === 'voice' && !hasTriggered.current) {
            hasTriggered.current = true;
            // Start listening immediately to preserve user activation context
            toggleListening();
        }
    }, [initialMode]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!input.trim() && !pendingImage) return;

        const currentInput = input;
        const currentImage = pendingImage;

        // Clear immediately
        setInput('');
        setPendingImage(null);

        // UI Message
        const userMsg = {
            id: Date.now(),
            role: 'user',
            content: currentInput,
            image: currentImage
        };
        setChatMessages(prev => [...prev, userMsg]);

        // Create history for context (last 6 chatMessages + current)
        // We strip images from history to save tokens, AI mostly needs text context for referencing
        const historyContext = [...chatMessages, userMsg].slice(-6).map(m => ({
            role: m.role,
            content: m.content
        }));

        try {
            let response;
            if (currentImage) {
                // Send Image + Optional Text
                response = await onSend({ image: currentImage, text: currentInput }, 'image', historyContext);
            } else {
                // Text only
                response = await onSend(currentInput, 'text', historyContext);
            }

            if (response) {
                setChatMessages(prev => [...prev, { id: Date.now(), role: 'ai', content: response.message }]);
            }
        } catch (error) {
            setChatMessages(prev => [...prev, { id: Date.now(), role: 'ai', content: "Sorry, something went wrong." }]);
        }
    };

    const handleFileSelect = async (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onloadend = async () => {
                const base64 = reader.result;
                setPendingImage(base64); // Stage it, don't send yet
            };
            reader.readAsDataURL(file);
        }
    };

    const handleRecipeSelect = async (recipe) => {
        const userMsg = { id: Date.now(), role: 'user', content: `Logged recipe: ${recipe.name}` };
        setChatMessages(prev => [...prev, userMsg]);
        setShowRecipes(false);

        try {
            const response = await onSend(recipe, 'recipe');
            if (response) {
                setChatMessages(prev => [...prev, { id: Date.now(), role: 'ai', content: response.message }]);
            }
        } catch (error) {
            setChatMessages(prev => [...prev, { id: Date.now(), role: 'ai', content: "Failed to log recipe." }]);
        }
    };

    const mediaRecorderRef = useRef(null);
    const audioChunksRef = useRef([]);

    // Handle initial actions (Camera/Voice shortcuts)
    useEffect(() => {
        if (initialMode === 'camera' && !hasTriggered.current) {
            hasTriggered.current = true;
            setTimeout(() => fileInputRef.current?.click(), 100);
        } else if (initialMode === 'voice' && !hasTriggered.current) {
            hasTriggered.current = true;
            startRecording();
        }
    }, [initialMode]);

    const startRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const mediaRecorder = new MediaRecorder(stream);
            mediaRecorderRef.current = mediaRecorder;
            audioChunksRef.current = [];

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    audioChunksRef.current.push(event.data);
                }
            };

            mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
                const reader = new FileReader();
                reader.readAsDataURL(audioBlob);
                reader.onloadend = async () => {
                    const base64Audio = reader.result;
                    setIsListening(false);
                    setInput("Transcribing..."); // Feedback in input

                    try {
                        const response = await onSend({ audio: base64Audio }, 'transcribe');
                        if (response && response.transcription) {
                            setInput(prev => (prev === "Transcribing..." ? "" : prev) + " " + response.transcription);
                        } else {
                            setInput(prev => (prev === "Transcribing..." ? "" : prev));
                            alert("Could not transcribe audio.");
                        }
                    } catch (error) {
                        console.error("Transcription failed", error);
                        setInput(prev => (prev === "Transcribing..." ? "" : prev));
                        alert("Transcription failed.");
                    }
                };

                // Stop all tracks to release mic
                stream.getTracks().forEach(track => track.stop());
            };

            mediaRecorder.start();
            setIsListening(true);
        } catch (err) {
            console.error("Error accessing microphone:", err);
            alert("Could not access microphone.");
            setIsListening(false);
        }
    };

    const stopRecording = () => {
        if (mediaRecorderRef.current && isListening) {
            mediaRecorderRef.current.stop();
            // isListening state is updated in onstop
        }
    };

    const toggleListening = () => {
        if (isListening) {
            stopRecording();
        } else {
            startRecording();
        }
    };

    // --- History Suggestions ---
    const historyItems = useMemo(() => {
        const unique = new Map();
        logs.forEach(log => {
            if (log.food_name && log.calories) {
                const key = log.food_name.toLowerCase().trim();
                if (!unique.has(key)) {
                    unique.set(key, { name: log.food_name, calories: log.calories });
                }
            }
        });
        return Array.from(unique.values());
    }, [logs]);

    const [suggestions, setSuggestions] = useState([]);

    const handleInputChange = (e) => {
        const val = e.target.value;
        setInput(val);

        if (val.length > 1) {
            const matches = historyItems
                .filter(item => item.name.toLowerCase().includes(val.toLowerCase()))
                .slice(0, 5);
            setSuggestions(matches);
        } else {
            setSuggestions([]);
        }
    };

    const selectSuggestion = (item) => {
        setInput(item.name);
        setSuggestions([]);
    };

    return (
        <div className="flex flex-col h-full w-full max-w-md mx-auto relative bg-neutral-900 md:rounded-3xl overflow-hidden shadow-2xl border-x md:border border-neutral-800">
            {/* Header (only if onClose provided) */}
            {onClose && (
                <div className="flex items-center justify-between p-4 bg-neutral-900 border-b border-neutral-800 z-10 sticky top-0">
                    <h2 className="text-xl font-bold flex-1 text-white">Food Tracker</h2>
                    <button
                        onClick={() => {
                            setChatMessages([{ id: 'welcome', role: 'ai', content: 'Hello! I can help you track calories. Send me a photo of your food, type what you ate, or just say it!' }]);
                        }}
                        className="text-xs text-neutral-500 hover:text-rose-400 transition-colors uppercase tracking-wider font-bold mr-4"
                    >
                        Clear Chat
                    </button>
                    <button onClick={onClose} className="p-2 hover:bg-neutral-700/50 rounded-full transition-colors text-neutral-400 hover:text-white">
                        <X size={20} />
                    </button>
                </div>
            )}

            {/* Feed */}
            <div className={`flex-1 overflow-y-auto p-4 space-y-4 pb-32 transition-all ${pendingImage ? 'pb-48' : ''}`}>
                {chatMessages.map((msg) => (
                    <div key={msg.id} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                        <div className={`max-w-[80%] p-4 rounded-2xl ${msg.role === 'user'
                            ? 'bg-rose-500 text-white rounded-tr-sm'
                            : 'bg-neutral-800 text-neutral-200 rounded-tl-sm'
                            }`}>
                            {msg.image && (
                                <img src={msg.image} alt="User upload" className="rounded-lg mb-2 max-h-48 object-cover" />
                            )}
                            <p className="whitespace-pre-wrap">{msg.content}</p>
                        </div>
                    </div>
                ))}
                <div ref={messagesEndRef} />
            </div>

            {/* Input Bar */}
            <div className="fixed bottom-0 left-0 right-0 p-4 bg-gradient-to-t from-neutral-900 via-neutral-900 to-transparent z-50">
                <div className="max-w-md mx-auto relative">

                    {/* Pending Image Preview */}
                    {pendingImage && (
                        <div className="mb-2 bg-neutral-800 rounded-2xl p-2 flex items-center gap-3 animate-in slide-in-from-bottom-2 border border-neutral-700">
                            <img src={pendingImage} alt="Preview" className="w-16 h-16 rounded-xl object-cover" />
                            <div className="flex-1">
                                <p className="text-xs text-neutral-400 font-bold uppercase">Attached Image</p>
                                <p className="text-xs text-neutral-500">Add context or send to analyze</p>
                            </div>
                            <button
                                onClick={() => setPendingImage(null)}
                                className="p-2 bg-neutral-700 hover:bg-neutral-600 rounded-full text-white transition-colors"
                            >
                                <X size={14} />
                            </button>
                        </div>
                    )}

                    {/* Suggestions Popup */}
                    {suggestions.length > 0 && !pendingImage && (
                        <div className="absolute bottom-full left-0 right-0 mb-2 bg-neutral-800 border border-neutral-700 rounded-2xl shadow-xl overflow-hidden animate-in fade-in slide-in-from-bottom-2">
                            <div className="px-3 py-2 border-b border-neutral-700/50 text-[10px] uppercase font-bold text-neutral-500 tracking-wider">
                                Suggestions from History
                            </div>
                            {suggestions.map((item, idx) => (
                                <button
                                    key={idx}
                                    onClick={() => selectSuggestion(item)}
                                    className="w-full text-left px-4 py-3 hover:bg-neutral-700/50 flex justify-between items-center transition-colors group"
                                >
                                    <span className="text-sm text-neutral-200 group-hover:text-white truncate">{item.name}</span>
                                    <span className="text-xs text-neutral-500 font-mono">{item.calories} kcal</span>
                                </button>
                            ))}
                        </div>
                    )}

                    <div className="bg-neutral-800/90 backdrop-blur-md border border-neutral-700 p-2 rounded-3xl flex items-end gap-2 shadow-2xl">
                        <div className="flex gap-1 pb-1">
                            <input
                                type="file"
                                accept="image/*"
                                className="hidden"
                                ref={fileInputRef}
                                onChange={handleFileSelect}
                            />
                            <button
                                onClick={() => fileInputRef.current?.click()}
                                className={`p-3 rounded-xl transition-colors ${pendingImage ? 'text-rose-500 bg-rose-500/10' : 'text-neutral-400 hover:text-white hover:bg-neutral-700/50'}`}
                            >
                                <Camera size={20} />
                            </button>
                            <button
                                className="p-3 text-neutral-400 hover:text-white hover:bg-neutral-700/50 rounded-xl transition-colors"
                                onClick={() => setShowRecipes(true)}
                            >
                                <BookOpen size={20} />
                            </button>
                            <button
                                className="p-3 text-neutral-400 hover:text-white hover:bg-neutral-700/50 rounded-xl transition-colors"
                                onClick={onOpenBarcode}
                            >
                                <ScanBarcode size={20} />
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="flex-1 flex items-center gap-2 bg-neutral-900/50 rounded-2xl p-2 border border-neutral-700/50 focus-within:border-rose-500/50 transition-colors">
                            <input
                                type="text"
                                value={input}
                                onChange={handleInputChange}
                                placeholder={isListening ? "Listening... (Tap to stop)" : (pendingImage ? "Ask about this image..." : "Type, paste URL, or speak...")}
                                className={`flex-1 bg-transparent border-none text-white placeholder-neutral-500 focus:ring-0 p-2 outline-none ${isListening ? 'animate-pulse text-rose-500 font-medium' : ''}`}
                                disabled={isListening}
                            />
                            {input.trim() || pendingImage ? (
                                <button
                                    type="submit"
                                    className="p-2 bg-rose-500 text-white rounded-xl hover:bg-rose-600 transition-colors"
                                >
                                    <Send size={18} />
                                </button>
                            ) : (
                                <button
                                    type="button"
                                    onClick={toggleListening}
                                    className={`p-2 rounded-xl transition-all ${isListening ? 'bg-rose-500 text-white animate-pulse shadow-lg shadow-rose-500/20' : 'text-neutral-400 hover:text-white'}`}
                                >
                                    {isListening ? <Square size={20} fill="currentColor" /> : <Mic size={20} />}
                                </button>
                            )}
                        </form>
                    </div>
                </div>
            </div>

            {/* Recipe Modal */}
            {showRecipes && (
                <RecipeList
                    onClose={() => setShowRecipes(false)}
                    onSelect={handleRecipeSelect}
                />
            )}
        </div>
    );
};

export default ChatInterface;
