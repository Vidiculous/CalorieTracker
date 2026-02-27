import React, { useState, useMemo } from 'react';
import { X, Plus, Clock, Zap } from 'lucide-react';
import { useAppContext } from '../context/AppContext';

const ManualEntryModal = ({ onClose, onAdd }) => {
    const { logs, addTemplate } = useAppContext();
    const [foodName, setFoodName] = useState('');
    const [calories, setCalories] = useState('');
    const [protein, setProtein] = useState('');
    const [carbs, setCarbs] = useState('');
    const [fat, setFat] = useState('');
    const [mealType, setMealType] = useState('');
    const [templateSaved, setTemplateSaved] = useState(false);

    const [suggestions, setSuggestions] = useState([]);

    // --- History Logic ---
    const historyItems = useMemo(() => {
        const unique = new Map();
        logs.forEach(log => {
            if (log.food_name && log.calories) {
                const key = log.food_name.toLowerCase().trim();
                if (!unique.has(key)) {
                    unique.set(key, {
                        name: log.food_name,
                        calories: log.calories,
                        protein: log.protein || 0,
                        carbs: log.carbs || 0,
                        fat: log.fat || 0
                    });
                }
            }
        });
        return Array.from(unique.values());
    }, [logs]);

    const handleNameChange = (e) => {
        const val = e.target.value;
        setFoodName(val);

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
        setFoodName(item.name);
        setCalories(item.calories);
        setProtein(item.protein);
        setCarbs(item.carbs);
        setFat(item.fat);
        setSuggestions([]);
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!foodName || !calories) return;

        onAdd({
            food_name: foodName,
            calories: parseInt(calories),
            protein: parseInt(protein) || 0,
            carbs: parseInt(carbs) || 0,
            fat: parseInt(fat) || 0,
            quantity: '1 serving',
            source: 'manual',
            meal_type: mealType || undefined
        });
        onClose();
    };

    const handleSaveTemplate = () => {
        if (!foodName || !calories) return;
        addTemplate({
            name: foodName,
            items: [{
                food_name: foodName,
                calories: parseInt(calories),
                protein: parseInt(protein) || 0,
                carbs: parseInt(carbs) || 0,
                fat: parseInt(fat) || 0,
                quantity: '1 serving',
            }],
        });
        setTemplateSaved(true);
        setTimeout(() => setTemplateSaved(false), 2000);
    };

    const canSubmit = foodName && calories;

    return (
        <div className="fixed inset-0 z-[60] bg-black/50 backdrop-blur-sm flex items-center justify-center p-4" onClick={onClose}>
            <div className="bg-neutral-900 border border-neutral-800 p-6 rounded-3xl w-full max-w-sm shadow-2xl relative" onClick={e => e.stopPropagation()}>
                <div className="flex justify-between items-center mb-6">
                    <h2 className="text-xl font-bold text-white flex items-center gap-2">
                        <span className="p-2 bg-rose-500/20 rounded-xl text-rose-500"><Plus size={20} /></span>
                        Manual Entry
                    </h2>
                    <button onClick={onClose} className="p-2 text-neutral-400 hover:text-white bg-neutral-800 hover:bg-neutral-700 rounded-xl transition-colors">
                        <X size={20} />
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div className="relative">
                        <label className="text-xs font-bold text-neutral-500 ml-1 uppercase">Food Name</label>
                        <input
                            type="text"
                            value={foodName}
                            onChange={handleNameChange}
                            placeholder="e.g. Oatmeal with Berries"
                            className="w-full bg-neutral-800 border border-neutral-800 focus:border-rose-500 rounded-xl p-3 text-white outline-none transition-colors"
                            autoFocus
                        />

                        {/* Suggestions Dropdown */}
                        {suggestions.length > 0 && (
                            <div className="absolute top-full left-0 right-0 mt-1 bg-neutral-800 border border-neutral-700 rounded-xl shadow-xl z-50 overflow-hidden">
                                <div className="px-3 py-2 border-b border-neutral-700/50 text-[10px] uppercase font-bold text-neutral-500 tracking-wider flex items-center gap-2">
                                    <Clock size={10} /> Recent Items
                                </div>
                                {suggestions.map((item, idx) => (
                                    <button
                                        key={idx}
                                        type="button"
                                        onClick={() => selectSuggestion(item)}
                                        className="w-full text-left px-3 py-2 hover:bg-neutral-700/50 flex justify-between items-center transition-colors group"
                                    >
                                        <span className="text-sm text-neutral-200 group-hover:text-white truncate">{item.name}</span>
                                        <div className="text-right">
                                            <span className="text-xs text-neutral-400 font-mono block">{item.calories} kcal</span>
                                        </div>
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Meal Type Selection */}
                    <div>
                        <label className="text-xs font-bold text-neutral-500 ml-1 uppercase">Meal Section (Optional)</label>
                        <div className="grid grid-cols-4 gap-2 mt-1">
                            {['Breakfast', 'Lunch', 'Dinner', 'Snacks'].map((type) => (
                                <button
                                    key={type}
                                    type="button"
                                    onClick={() => setMealType(current => current === type ? '' : type)}
                                    className={`p-2 rounded-xl text-xs font-medium transition-all border ${mealType === type
                                        ? 'bg-rose-500 text-white border-rose-500'
                                        : 'bg-neutral-800 text-neutral-400 border-neutral-800 hover:border-neutral-600'
                                        }`}
                                >
                                    {type}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="text-xs font-bold text-neutral-500 ml-1 uppercase">Calories</label>
                            <input
                                type="number"
                                value={calories}
                                onChange={e => setCalories(e.target.value)}
                                placeholder="kcal"
                                className="w-full bg-neutral-800 border border-neutral-800 focus:border-rose-500 rounded-xl p-3 text-white outline-none transition-colors"
                            />
                        </div>
                        <div>
                            <label className="text-xs font-bold text-neutral-500 ml-1 uppercase">Protein (g)</label>
                            <input
                                type="number"
                                value={protein}
                                onChange={e => setProtein(e.target.value)}
                                placeholder="g"
                                className="w-full bg-neutral-800 border border-neutral-800 focus:border-rose-500 rounded-xl p-3 text-white outline-none transition-colors"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                        <div>
                            <label className="text-xs font-bold text-neutral-500 ml-1 uppercase">Carbs (g)</label>
                            <input
                                type="number"
                                value={carbs}
                                onChange={e => setCarbs(e.target.value)}
                                placeholder="g"
                                className="w-full bg-neutral-800 border border-neutral-800 focus:border-rose-500 rounded-xl p-3 text-white outline-none transition-colors"
                            />
                        </div>
                        <div>
                            <label className="text-xs font-bold text-neutral-500 ml-1 uppercase">Fat (g)</label>
                            <input
                                type="number"
                                value={fat}
                                onChange={e => setFat(e.target.value)}
                                placeholder="g"
                                className="w-full bg-neutral-800 border border-neutral-800 focus:border-rose-500 rounded-xl p-3 text-white outline-none transition-colors"
                            />
                        </div>
                    </div>

                    <button
                        type="submit"
                        disabled={!canSubmit}
                        className="w-full bg-rose-500 hover:bg-rose-600 disabled:opacity-50 disabled:cursor-not-allowed text-white p-4 rounded-2xl font-bold shadow-lg shadow-rose-500/20 transition-all active:scale-[0.98] mt-4"
                    >
                        Add Entry
                    </button>

                    <button
                        type="button"
                        onClick={handleSaveTemplate}
                        disabled={!canSubmit}
                        className={`w-full flex items-center justify-center gap-2 p-3 rounded-2xl font-bold text-sm transition-all active:scale-[0.98] border ${templateSaved
                            ? 'bg-amber-500/20 text-amber-400 border-amber-500/40'
                            : 'bg-neutral-800 text-neutral-400 border-neutral-700 hover:border-amber-500/50 hover:text-amber-400 disabled:opacity-40 disabled:cursor-not-allowed'
                            }`}
                    >
                        <Zap size={15} />
                        {templateSaved ? 'Template Saved!' : 'Save as Template'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default ManualEntryModal;
