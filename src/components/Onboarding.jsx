
import React, { useState } from 'react';
import { useAppContext } from '../context/AppContext';
import { ArrowRight, Flame } from 'lucide-react';

const Onboarding = () => {
    const { updateSettings } = useAppContext();
    const [goal, setGoal] = useState(2500);
    const [selectedModel, setSelectedModel] = useState('gemini-3-flash-preview');

    const handleStart = () => {
        updateSettings({
            dailyGoal: parseInt(goal),
            selectedModel,
            hasOnboarded: true
        });
    };

    return (
        <div className="min-h-screen bg-neutral-900 text-white flex flex-col justify-center items-center p-6 text-center">

            <div className="mb-8 p-4 bg-rose-500/10 rounded-full">
                <Flame size={48} className="text-rose-500" />
            </div>

            <h1 className="text-4xl font-bold mb-4 bg-gradient-to-br from-white to-neutral-400 bg-clip-text text-transparent">
                Welcome
            </h1>
            <p className="text-neutral-400 mb-12 max-w-xs">
                Let's set up your daily goals and AI preferences to get started.
            </p>

            <div className="w-full max-w-xs space-y-6">
                <div className="space-y-2 text-left">
                    <label className="text-sm font-medium text-neutral-300">Daily Calorie Goal</label>
                    <input
                        type="number"
                        value={goal}
                        onChange={(e) => setGoal(e.target.value)}
                        className="w-full bg-neutral-800 border-none rounded-2xl p-4 text-xl font-bold text-center focus:ring-2 focus:ring-rose-500 transition-all outline-none"
                    />
                </div>

                <div className="space-y-2 text-left">
                    <label className="text-sm font-medium text-neutral-300">AI Model</label>
                    <div className="relative">
                        <select
                            value={selectedModel}
                            onChange={(e) => setSelectedModel(e.target.value)}
                            className="w-full bg-neutral-800 border-none rounded-2xl p-4 appearance-none focus:ring-2 focus:ring-rose-500 transition-all outline-none"
                        >
                            <option value="gemini-3-flash-preview">Gemini 3 Flash (Preview)</option>
                            <option value="gemini-2.5-flash">Gemini 2.5 Flash (Stable)</option>
                            <option value="gemini-2.0-flash">Gemini 2.0 Flash</option>
                        </select>
                        <div className="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none text-neutral-500">
                            ▼
                        </div>
                    </div>
                </div>

                <button
                    onClick={handleStart}
                    className="w-full bg-rose-500 hover:bg-rose-600 text-white p-4 rounded-2xl font-bold text-lg flex items-center justify-center gap-2 transition-all active:scale-95"
                >
                    Get Started <ArrowRight size={20} />
                </button>
            </div>
        </div>
    );
};

export default Onboarding;
