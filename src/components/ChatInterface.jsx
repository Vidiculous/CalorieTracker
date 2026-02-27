import React, { useState, useRef, useEffect, useMemo, useCallback } from 'react';
import { Send, Mic, Camera, BookOpen, ScanBarcode, X, Square, Keyboard } from 'lucide-react';
import { useAppContext } from '../context/AppContext';
import RecipeList from './RecipeList';

const supportsWebSpeech = () => !!(window.SpeechRecognition || window.webkitSpeechRecognition);

const resizeImage = (dataUrl, maxPx = 1024, quality = 0.85) =>
    new Promise((resolve) => {
        const img = new Image();
        img.onload = () => {
            const scale = Math.min(1, maxPx / Math.max(img.width, img.height));
            const canvas = document.createElement('canvas');
            canvas.width = Math.round(img.width * scale);
            canvas.height = Math.round(img.height * scale);
            canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
            resolve(canvas.toDataURL('image/jpeg', quality));
        };
        img.src = dataUrl;
    });

const ChatInterface = ({ onSend, onOpenBarcode, onClose, initialMode }) => {
    const { logs, chatMessages, setChatMessages, settings, updateSettings } = useAppContext();
    const [input, setInput] = useState('');
    const [isListening, setIsListening] = useState(false);
    const [isTranscribing, setIsTranscribing] = useState(false);
    const [voiceError, setVoiceError] = useState(null);
    const [showRecipes, setShowRecipes] = useState(false);
    const [pendingImage, setPendingImage] = useState(null);
    const [suggestions, setSuggestions] = useState([]);
    const [audioLevel, setAudioLevel] = useState(0);

    const fileInputRef = useRef(null);
    const messagesEndRef = useRef(null);
    const mediaRecorderRef = useRef(null);
    const audioChunksRef = useRef([]);
    const recognitionRef = useRef(null);
    const interimTextRef = useRef('');
    const timeoutRef = useRef(null);
    const streamRef = useRef(null);
    const mimeTypeRef = useRef('audio/webm');
    const hasTriggered = useRef(false);
    const autoSubmitRef = useRef(settings.autoSubmit ?? true);

    // Audio meter refs
    const analyserRef = useRef(null);
    const animFrameRef = useRef(null);
    const audioCtxRef = useRef(null);
    const meterStreamRef = useRef(null);

    // Keep autoSubmitRef in sync with settings
    useEffect(() => {
        autoSubmitRef.current = settings.autoSubmit ?? true;
    }, [settings.autoSubmit]);

    const scrollToBottom = () => {
        messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    };

    useEffect(() => {
        scrollToBottom();
    }, [chatMessages, pendingImage]);

    // --- Audio Meter ---
    const startAudioMeter = useCallback((stream) => {
        try {
            const ctx = new AudioContext();
            audioCtxRef.current = ctx;
            const source = ctx.createMediaStreamSource(stream);
            const analyser = ctx.createAnalyser();
            analyser.fftSize = 256;
            source.connect(analyser);
            analyserRef.current = analyser;

            const data = new Uint8Array(analyser.frequencyBinCount);
            const tick = () => {
                analyser.getByteFrequencyData(data);
                const rms = Math.sqrt(data.reduce((sum, v) => sum + v * v, 0) / data.length);
                setAudioLevel(Math.min(1, rms / 100));
                animFrameRef.current = requestAnimationFrame(tick);
            };
            animFrameRef.current = requestAnimationFrame(tick);
        } catch {
            // AudioContext unavailable — skip metering
        }
    }, []);

    const stopAudioMeter = useCallback(() => {
        if (animFrameRef.current) {
            cancelAnimationFrame(animFrameRef.current);
            animFrameRef.current = null;
        }
        if (audioCtxRef.current) {
            audioCtxRef.current.close().catch(() => {});
            audioCtxRef.current = null;
        }
        analyserRef.current = null;
        setAudioLevel(0);
    }, []);

    const stopMeterStream = useCallback(() => {
        if (meterStreamRef.current) {
            meterStreamRef.current.getTracks().forEach(t => t.stop());
            meterStreamRef.current = null;
        }
    }, []);

    // Submit voice transcript directly to Gemini, bypassing React state timing
    const handleVoiceSubmit = useCallback(async (transcript) => {
        if (!transcript) return;

        const userMsg = { id: Date.now(), role: 'user', content: transcript };
        setChatMessages(prev => [...prev, userMsg]);
        setInput('');

        const historyContext = [...chatMessages, userMsg].slice(-6).map(m => ({
            role: m.role,
            content: m.content
        }));

        try {
            const response = await onSend(transcript, 'text', historyContext);
            if (response) {
                setChatMessages(prev => [...prev, { id: Date.now(), role: 'ai', content: response.message }]);
            }
        } catch {
            setChatMessages(prev => [...prev, { id: Date.now(), role: 'ai', content: "Sorry, something went wrong." }]);
        }
    }, [chatMessages, onSend, setChatMessages]);

    const startWebSpeechRecognition = useCallback(() => {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        const recognition = new SpeechRecognition();
        recognitionRef.current = recognition;
        interimTextRef.current = '';

        recognition.interimResults = true;
        recognition.continuous = false;

        recognition.onresult = (event) => {
            let finalText = '';
            let interimText = '';
            for (let i = event.resultIndex; i < event.results.length; i++) {
                const t = event.results[i][0].transcript;
                if (event.results[i].isFinal) {
                    finalText += t;
                } else {
                    interimText += t;
                }
            }
            if (finalText) interimTextRef.current = finalText;
            setInput(finalText || interimText);
        };

        recognition.onend = () => {
            clearTimeout(timeoutRef.current);
            setIsListening(false);
            stopAudioMeter();
            stopMeterStream();
            const transcript = interimTextRef.current.trim();
            recognitionRef.current = null;

            if (!transcript) {
                setInput('');
                return;
            }

            if (autoSubmitRef.current) {
                handleVoiceSubmit(transcript);
            } else {
                setInput(transcript);
            }
        };

        recognition.onerror = (event) => {
            clearTimeout(timeoutRef.current);
            setIsListening(false);
            stopAudioMeter();
            stopMeterStream();
            recognitionRef.current = null;
            if (event.error === 'no-speech' || event.error === 'aborted') return;
            setVoiceError(`Microphone error: ${event.error}`);
        };

        // 10s safety timeout
        timeoutRef.current = setTimeout(() => {
            recognition.stop();
        }, 10000);

        recognition.start();
        setIsListening(true);
        setVoiceError(null);

        // Try to get a separate audio stream for metering (silently fails if denied)
        if (navigator.mediaDevices) {
            navigator.mediaDevices.getUserMedia({ audio: true }).then(stream => {
                meterStreamRef.current = stream;
                startAudioMeter(stream);
            }).catch(() => {});
        }
    }, [handleVoiceSubmit, startAudioMeter, stopAudioMeter, stopMeterStream]);

    const startFallbackRecording = useCallback(async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            streamRef.current = stream;
            startAudioMeter(stream);

            const preferredTypes = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4', 'audio/ogg'];
            const mimeType = preferredTypes.find(t => MediaRecorder.isTypeSupported(t)) || '';
            mimeTypeRef.current = mimeType || 'audio/webm';

            const mediaRecorder = new MediaRecorder(stream, mimeType ? { mimeType } : {});
            mediaRecorderRef.current = mediaRecorder;
            audioChunksRef.current = [];

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    audioChunksRef.current.push(event.data);
                }
            };

            mediaRecorder.onstop = async () => {
                stopAudioMeter();
                const audioBlob = new Blob(audioChunksRef.current, { type: mimeTypeRef.current });
                const reader = new FileReader();
                reader.readAsDataURL(audioBlob);
                reader.onloadend = async () => {
                    const base64Audio = reader.result;
                    setIsTranscribing(true);

                    timeoutRef.current = setTimeout(() => {
                        setIsTranscribing(false);
                        setVoiceError('Transcription timed out. Please try again.');
                    }, 10000);

                    try {
                        const response = await onSend({ audio: base64Audio, mimeType: mimeTypeRef.current }, 'transcribe');
                        clearTimeout(timeoutRef.current);
                        setIsTranscribing(false);
                        if (response && response.transcription) {
                            setInput(response.transcription.trim());
                        } else {
                            setVoiceError('Could not transcribe audio.');
                        }
                    } catch (error) {
                        clearTimeout(timeoutRef.current);
                        console.error("Transcription failed", error);
                        setIsTranscribing(false);
                        setVoiceError('Transcription failed.');
                    }
                };

                stream.getTracks().forEach(track => track.stop());
            };

            mediaRecorder.start();
            setIsListening(true);
            setVoiceError(null);
        } catch (err) {
            console.error("Error accessing microphone:", err);
            setVoiceError('Could not access microphone.');
            setIsListening(false);
        }
    }, [onSend, startAudioMeter, stopAudioMeter]);

    const stopFallbackRecording = useCallback(() => {
        if (mediaRecorderRef.current && mediaRecorderRef.current.state !== 'inactive') {
            mediaRecorderRef.current.stop();
            setIsListening(false);
        }
    }, []);

    const toggleListening = useCallback(() => {
        if (isListening) {
            if (recognitionRef.current) {
                recognitionRef.current.stop();
            } else {
                stopFallbackRecording();
            }
        } else {
            if (supportsWebSpeech()) {
                startWebSpeechRecognition();
            } else {
                startFallbackRecording();
            }
        }
    }, [isListening, startWebSpeechRecognition, startFallbackRecording, stopFallbackRecording]);

    // Merged initialMode handler + cleanup
    useEffect(() => {
        if (initialMode === 'camera' && !hasTriggered.current) {
            hasTriggered.current = true;
            setTimeout(() => fileInputRef.current?.click(), 100);
        } else if (initialMode === 'voice' && !hasTriggered.current) {
            hasTriggered.current = true;
            setTimeout(() => toggleListening(), 100);
        }

        return () => {
            recognitionRef.current?.abort();
            if (mediaRecorderRef.current?.state !== 'inactive') {
                mediaRecorderRef.current?.stop();
            }
            streamRef.current?.getTracks().forEach(t => t.stop());
            clearTimeout(timeoutRef.current);
            stopAudioMeter();
            stopMeterStream();
        };
    }, [initialMode, toggleListening, stopAudioMeter, stopMeterStream]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!input.trim() && !pendingImage) return;

        const currentInput = input;
        const currentImage = pendingImage;

        setInput('');
        setPendingImage(null);

        const userMsg = {
            id: Date.now(),
            role: 'user',
            content: currentInput,
            image: currentImage
        };
        setChatMessages(prev => [...prev, userMsg]);

        const historyContext = [...chatMessages, userMsg].slice(-6).map(m => ({
            role: m.role,
            content: m.content
        }));

        try {
            let response;
            if (currentImage) {
                response = await onSend({ image: currentImage, text: currentInput }, 'image', historyContext);
            } else {
                response = await onSend(currentInput, 'text', historyContext);
            }
            if (response) {
                setChatMessages(prev => [...prev, { id: Date.now(), role: 'ai', content: response.message }]);
            }
        } catch {
            setChatMessages(prev => [...prev, { id: Date.now(), role: 'ai', content: "Sorry, something went wrong." }]);
        }
    };

    const handleFileSelect = (e) => {
        const file = e.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onloadend = async () => {
                setPendingImage(await resizeImage(reader.result));
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
        } catch {
            setChatMessages(prev => [...prev, { id: Date.now(), role: 'ai', content: "Failed to log recipe." }]);
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

    const toggleAutoSubmit = () => {
        updateSettings({ autoSubmit: !(settings.autoSubmit ?? true) });
    };

    const autoSubmitOn = settings.autoSubmit ?? true;

    // Mic button scale based on audio level
    const micScale = isListening ? 1 + audioLevel * 0.4 : 1;

    return (
        <div className="flex flex-col h-full w-full max-w-md mx-auto relative bg-neutral-900 md:rounded-3xl overflow-hidden shadow-2xl border-x md:border border-neutral-800">
            {/* Header */}
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

                    {/* Voice error hint */}
                    {voiceError && <p className="text-xs text-rose-400 mb-1 px-3">{voiceError}</p>}

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
                                placeholder={
                                    isListening ? "Listening... (tap to stop)" :
                                    isTranscribing ? "Transcribing..." :
                                    (pendingImage ? "Ask about this image..." : "Type, paste URL, or speak...")
                                }
                                className={`flex-1 bg-transparent border-none text-white placeholder-neutral-500 focus:ring-0 p-2 outline-none ${isTranscribing ? 'animate-pulse text-rose-400' : ''}`}
                                disabled={isTranscribing}
                            />
                            {!isListening && supportsWebSpeech() && (
                                <button
                                    type="button"
                                    onClick={toggleAutoSubmit}
                                    title={autoSubmitOn ? "Auto-submit ON: tap to keep text in field instead" : "Auto-submit OFF: tap to send voice immediately"}
                                    className={`p-1.5 rounded-lg transition-colors ${autoSubmitOn ? 'text-rose-400 hover:text-rose-300' : 'text-neutral-500 hover:text-neutral-300'}`}
                                >
                                    {autoSubmitOn ? <Send size={14} /> : <Keyboard size={14} />}
                                </button>
                            )}
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
                                    style={{ transform: `scale(${micScale})` }}
                                    className={`p-2 rounded-xl transition-colors ${isListening ? 'bg-rose-500 text-white shadow-lg shadow-rose-500/20' : 'text-neutral-400 hover:text-white'}`}
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
