import React, { useState, useEffect } from 'react';
import { X, Search, Loader2 } from 'lucide-react';
import { searchFoods } from '../services/foodApi';

const MEAL_TYPES = ['Breakfast', 'Lunch', 'Dinner', 'Snacks'];

const FoodSearch = ({ onClose, onAdd }) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [selected, setSelected] = useState(null);
    const [serving, setServing] = useState(100);
    const [mealType, setMealType] = useState('');

    useEffect(() => {
        const timer = setTimeout(async () => {
            if (!query.trim()) {
                setResults([]);
                setError(null);
                return;
            }
            setLoading(true);
            setError(null);
            const data = await searchFoods(query);
            setResults(data);
            if (data.length === 0) setError('No results found');
            setLoading(false);
        }, 300);
        return () => clearTimeout(timer);
    }, [query]);

    const scale = serving / 100;

    const handleAdd = () => {
        if (!selected) return;
        onAdd({
            food_name: selected.food_name + (selected.brand ? ` (${selected.brand})` : ''),
            calories: Math.round(selected.calories * scale),
            protein: Math.round(selected.protein * scale),
            carbs: Math.round(selected.carbs * scale),
            fat: Math.round(selected.fat * scale),
            quantity: `${serving}g`,
            source: 'fooddb',
            meal_type: mealType || undefined,
        });
        onClose();
    };

    return (
        <div className="fixed inset-0 z-[80] bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4" onClick={onClose}>
            <div className="bg-neutral-900 border border-neutral-800 rounded-t-3xl sm:rounded-3xl w-full sm:max-w-md max-h-[90vh] flex flex-col shadow-2xl" onClick={e => e.stopPropagation()}>

                {/* Header */}
                <div className="flex items-center justify-between p-5 border-b border-neutral-800 shrink-0">
                    <h2 className="text-xl font-bold text-white">Food Search</h2>
                    <button onClick={onClose} className="p-2 text-neutral-400 hover:text-white bg-neutral-800 hover:bg-neutral-700 rounded-xl transition-colors">
                        <X size={20} />
                    </button>
                </div>

                {/* Search Input */}
                <div className="p-4 border-b border-neutral-800 shrink-0">
                    <div className="relative">
                        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-500" />
                        <input
                            type="text"
                            value={query}
                            onChange={e => { setQuery(e.target.value); setSelected(null); }}
                            placeholder="Search foods (e.g. oats, chicken)..."
                            autoFocus
                            className="w-full bg-neutral-800 border border-neutral-700 focus:border-rose-500 rounded-xl pl-9 pr-10 py-3 text-white outline-none transition-colors placeholder-neutral-500 text-sm"
                        />
                        {loading && <Loader2 size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-neutral-500 animate-spin" />}
                    </div>
                </div>

                {/* Content */}
                <div className="flex-1 overflow-y-auto">
                    {!selected ? (
                        <>
                            {!query && !loading && (
                                <p className="text-center text-neutral-600 text-sm py-10">Start typing to search the food database</p>
                            )}
                            {error && !loading && (
                                <p className="text-center text-neutral-500 text-sm py-10">{error}</p>
                            )}
                            {results.map((item, idx) => (
                                <button
                                    key={idx}
                                    onClick={() => { setSelected(item); setServing(100); }}
                                    className="w-full text-left px-4 py-3 hover:bg-neutral-800/70 transition-colors border-b border-neutral-800/50 last:border-0"
                                >
                                    <div className="flex justify-between items-start">
                                        <div className="flex-1 min-w-0 mr-3">
                                            <p className="text-white font-medium text-sm truncate">{item.food_name}</p>
                                            {item.brand && <p className="text-neutral-500 text-xs truncate">{item.brand}</p>}
                                        </div>
                                        <div className="text-right shrink-0">
                                            <span className="text-white font-bold text-sm">{item.calories} kcal</span>
                                            <p className="text-[10px] text-neutral-500">/ 100g</p>
                                        </div>
                                    </div>
                                    <div className="flex gap-3 mt-1 text-[11px]">
                                        <span className="text-blue-400">P {item.protein}g</span>
                                        <span className="text-amber-400">C {item.carbs}g</span>
                                        <span className="text-rose-400">F {item.fat}g</span>
                                    </div>
                                </button>
                            ))}
                        </>
                    ) : (
                        <div className="p-4 space-y-4">
                            <button
                                onClick={() => setSelected(null)}
                                className="text-sm text-neutral-500 hover:text-white transition-colors"
                            >
                                ← Back to results
                            </button>

                            <div>
                                <p className="text-white font-bold">{selected.food_name}</p>
                                {selected.brand && <p className="text-neutral-500 text-sm">{selected.brand}</p>}
                            </div>

                            {/* Serving size */}
                            <div>
                                <label className="text-xs font-bold text-neutral-500 uppercase block mb-1">Serving Size (g)</label>
                                <input
                                    type="number"
                                    value={serving}
                                    onChange={e => setServing(Math.max(1, Number(e.target.value)))}
                                    className="w-full bg-neutral-800 border border-neutral-700 focus:border-rose-500 rounded-xl p-3 text-white outline-none transition-colors"
                                />
                            </div>

                            {/* Scaled nutrition */}
                            <div className="bg-neutral-800/60 rounded-2xl p-4 grid grid-cols-4 gap-2 text-center">
                                <div>
                                    <p className="text-white font-bold text-sm">{Math.round(selected.calories * scale)}</p>
                                    <p className="text-neutral-500 text-xs">kcal</p>
                                </div>
                                <div>
                                    <p className="text-blue-400 font-bold text-sm">{Math.round(selected.protein * scale)}g</p>
                                    <p className="text-neutral-500 text-xs">protein</p>
                                </div>
                                <div>
                                    <p className="text-amber-400 font-bold text-sm">{Math.round(selected.carbs * scale)}g</p>
                                    <p className="text-neutral-500 text-xs">carbs</p>
                                </div>
                                <div>
                                    <p className="text-rose-400 font-bold text-sm">{Math.round(selected.fat * scale)}g</p>
                                    <p className="text-neutral-500 text-xs">fat</p>
                                </div>
                            </div>

                            {/* Meal type */}
                            <div>
                                <label className="text-xs font-bold text-neutral-500 uppercase block mb-1">Meal (Optional)</label>
                                <div className="grid grid-cols-4 gap-2">
                                    {MEAL_TYPES.map(type => (
                                        <button
                                            key={type}
                                            type="button"
                                            onClick={() => setMealType(curr => curr === type ? '' : type)}
                                            className={`p-2 rounded-xl text-xs font-medium transition-all border ${mealType === type ? 'bg-rose-500 text-white border-rose-500' : 'bg-neutral-800 text-neutral-400 border-neutral-700 hover:border-neutral-600'}`}
                                        >
                                            {type}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <button
                                onClick={handleAdd}
                                className="w-full bg-rose-500 hover:bg-rose-600 text-white p-4 rounded-2xl font-bold transition-all active:scale-[0.98] shadow-lg shadow-rose-500/20"
                            >
                                Log {Math.round(selected.calories * scale)} kcal
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default FoodSearch;
