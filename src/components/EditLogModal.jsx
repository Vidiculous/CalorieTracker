import React, { useState } from 'react';
import { X, Trash2, Check } from 'lucide-react';

const EditLogModal = ({ entry, onSave, onDelete, onClose }) => {
    const [formData, setFormData] = useState({
        food_name: entry.food_name,
        calories: entry.calories,
        protein: entry.protein,
        carbs: entry.carbs || 0,
        fat: entry.fat || 0,
    });

    const getMealType = (dateStr) => {
        const hour = new Date(dateStr).getHours();
        if (hour >= 5 && hour < 11) return 'Breakfast';
        if (hour >= 11 && hour < 16) return 'Lunch';
        if (hour >= 16 && hour < 22) return 'Dinner';
        return 'Snacks';
    };

    const inferredType = getMealType(entry.timestamp);

    // Initialize with existing meal_type if present
    const [mealType, setMealType] = useState(entry.meal_type || '');

    const effectiveType = mealType || inferredType;

    const [items, setItems] = useState(entry.items || []);

    // Calculate totals if items exist
    const isComposite = items.length > 0;

    // Derived totals for composite meals
    const totals = isComposite ? items.reduce((acc, item) => ({
        calories: acc.calories + (Number(item.calories) || 0),
        protein: acc.protein + (Number(item.protein) || 0),
        carbs: acc.carbs + (Number(item.carbs) || 0),
        fat: acc.fat + (Number(item.fat) || 0),
    }), { calories: 0, protein: 0, carbs: 0, fat: 0 }) : formData;

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: name === 'food_name' ? value : Number(value)
        }));
    };

    const handleItemChange = (id, field, value) => {
        setItems(prev => prev.map(item =>
            item.id === id ? { ...item, [field]: field === 'food_name' ? value : Number(value) } : item
        ));
    };

    const handleItemDelete = (id) => {
        setItems(prev => prev.filter(item => item.id !== id));
    };

    const handleSubmit = () => {
        if (isComposite) {
            if (items.length === 0) {
                onDelete(entry.id);
                return;
            }
            onSave(entry.id, {
                food_name: formData.food_name,
                ...totals,
                items: items,
                meal_type: mealType || undefined
            });
        } else {
            onSave(entry.id, {
                ...formData,
                meal_type: mealType || undefined
            });
        }
    };

    return (
        <div className="fixed inset-0 z-[70] bg-black/60 backdrop-blur-md flex items-center justify-center p-4">
            <div className="bg-neutral-900 border border-neutral-800 w-full max-w-md rounded-3xl p-6 shadow-2xl space-y-4 animate-in zoom-in-95 duration-200 max-h-[85vh] overflow-y-auto">
                <div className="flex justify-between items-center mb-2">
                    <h3 className="text-lg font-bold text-white">Edit Entry</h3>
                    <button onClick={onClose} className="p-2 hover:bg-neutral-800 rounded-full text-neutral-400 hover:text-white transition-colors">
                        <X size={20} />
                    </button>
                </div>

                <div className="space-y-4">
                    <input
                        type="text"
                        name="food_name"
                        value={formData.food_name}
                        onChange={handleChange}
                        className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-3 text-white focus:border-rose-500 outline-none text-lg font-bold"
                        placeholder="Meal Name"
                    />

                    {/* Meal Type Selection */}
                    <div>
                        <label className="text-xs font-bold text-neutral-500 ml-1 uppercase">Meal Section</label>
                        <div className="grid grid-cols-4 gap-2 mt-1">
                            {['Breakfast', 'Lunch', 'Dinner', 'Snacks'].map((type) => (
                                <button
                                    key={type}
                                    type="button"
                                    onClick={() => setMealType(current => current === type ? '' : type)}
                                    className={`p-2 rounded-xl text-[10px] font-bold uppercase transition-all border ${effectiveType === type
                                        ? 'bg-rose-500 text-white border-rose-500'
                                        : 'bg-neutral-800 text-neutral-400 border-neutral-800 hover:border-neutral-600'
                                        }`}
                                >
                                    {type}
                                </button>
                            ))}
                        </div>
                        <p className="text-[10px] text-neutral-600 ml-1 mt-1">
                            {mealType ? 'Manual Override Active' : 'Using time-based sorting (Auto)'}
                        </p>
                    </div>

                    {/* Totals Section */}
                    <div className="grid grid-cols-4 gap-2">
                        <div className="bg-neutral-800 p-2 rounded-xl text-center">
                            <span className="text-[10px] text-neutral-500 font-bold block">KCAL</span>
                            <span className="text-white font-bold">{isComposite ? totals.calories : formData.calories}</span>
                        </div>
                        <div className="bg-neutral-800 p-2 rounded-xl text-center">
                            <span className="text-[10px] text-neutral-500 font-bold block">PROT</span>
                            <span className="text-blue-400 font-bold">{isComposite ? totals.protein : formData.protein}g</span>
                        </div>
                        <div className="bg-neutral-800 p-2 rounded-xl text-center">
                            <span className="text-[10px] text-neutral-500 font-bold block">CARBS</span>
                            <span className="text-amber-400 font-bold">{isComposite ? totals.carbs : formData.carbs}g</span>
                        </div>
                        <div className="bg-neutral-800 p-2 rounded-xl text-center">
                            <span className="text-[10px] text-neutral-500 font-bold block">FAT</span>
                            <span className="text-rose-400 font-bold">{isComposite ? totals.fat : formData.fat}g</span>
                        </div>
                    </div>

                    {!isComposite && (
                        <div className="grid grid-cols-2 gap-3">
                            <div>
                                <label className="text-xs text-neutral-500 font-medium ml-1">Calories</label>
                                <input type="number" name="calories" value={formData.calories} onChange={handleChange} className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-3 text-white focus:border-rose-500 outline-none" />
                            </div>
                            <div>
                                <label className="text-xs text-neutral-500 font-medium ml-1">Protein (g)</label>
                                <input type="number" name="protein" value={formData.protein} onChange={handleChange} className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-3 text-white focus:border-blue-500 outline-none" />
                            </div>
                            <div>
                                <label className="text-xs text-neutral-500 font-medium ml-1">Carbs (g)</label>
                                <input type="number" name="carbs" value={formData.carbs} onChange={handleChange} className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-3 text-white focus:border-amber-500 outline-none" />
                            </div>
                            <div>
                                <label className="text-xs text-neutral-500 font-medium ml-1">Fat (g)</label>
                                <input type="number" name="fat" value={formData.fat} onChange={handleChange} className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-3 text-white focus:border-rose-400 outline-none" />
                            </div>
                        </div>
                    )}

                    {isComposite && (
                        <div className="space-y-3 mt-4">
                            <label className="text-xs text-neutral-500 font-bold tracking-wider uppercase ml-1">Ingredients</label>
                            {items.map((item, idx) => (
                                <div key={item.id || idx} className="bg-neutral-900/30 p-3 rounded-xl border border-neutral-800 space-y-2">
                                    <div className="flex gap-2">
                                        <input
                                            type="text"
                                            value={item.food_name}
                                            onChange={(e) => handleItemChange(item.id, 'food_name', e.target.value)}
                                            className="flex-1 bg-transparent border-b border-neutral-800 rounded-none p-2 text-sm text-white focus:border-neutral-500 outline-none placeholder-neutral-700"
                                            placeholder="Item Name"
                                        />
                                        <button
                                            onClick={() => handleItemDelete(item.id)}
                                            className="p-2 text-neutral-600 hover:text-red-500 transition-colors"
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </div>
                                    <div className="grid grid-cols-4 gap-2">
                                        <div>
                                            <input
                                                type="number"
                                                value={item.calories}
                                                onChange={(e) => handleItemChange(item.id, 'calories', e.target.value)}
                                                className="w-full bg-neutral-800/50 border border-neutral-800 rounded-lg p-1.5 text-xs text-white text-center outline-none focus:border-neutral-600"
                                                placeholder="Kcal"
                                            />
                                            <span className="text-[9px] text-neutral-600 text-center block mt-1">kcal</span>
                                        </div>
                                        <div>
                                            <input
                                                type="number"
                                                value={item.protein}
                                                onChange={(e) => handleItemChange(item.id, 'protein', e.target.value)}
                                                className="w-full bg-neutral-800/50 border border-neutral-800 rounded-lg p-1.5 text-xs text-white text-center outline-none focus:border-blue-900/50"
                                                placeholder="P"
                                            />
                                            <span className="text-[9px] text-neutral-600 text-center block mt-1">Prot</span>
                                        </div>
                                        <div>
                                            <input
                                                type="number"
                                                value={item.carbs || 0}
                                                onChange={(e) => handleItemChange(item.id, 'carbs', e.target.value)}
                                                className="w-full bg-neutral-800/50 border border-neutral-800 rounded-lg p-1.5 text-xs text-white text-center outline-none focus:border-amber-900/50"
                                                placeholder="C"
                                            />
                                            <span className="text-[9px] text-neutral-600 text-center block mt-1">Carb</span>
                                        </div>
                                        <div>
                                            <input
                                                type="number"
                                                value={item.fat || 0}
                                                onChange={(e) => handleItemChange(item.id, 'fat', e.target.value)}
                                                className="w-full bg-neutral-800/50 border border-neutral-800 rounded-lg p-1.5 text-xs text-white text-center outline-none focus:border-rose-900/50"
                                                placeholder="F"
                                            />
                                            <span className="text-[9px] text-neutral-600 text-center block mt-1">Fat</span>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="flex gap-3 mt-4 pt-2">
                    <button
                        onClick={() => onDelete(entry.id)}
                        className="flex-1 bg-red-500/10 text-red-500 hover:bg-red-500 hover:text-white p-3 rounded-xl font-bold flex items-center justify-center gap-2 transition-colors"
                    >
                        <Trash2 size={18} /> Delete Meal
                    </button>
                    <button
                        onClick={handleSubmit}
                        className="flex-1 bg-white text-black hover:bg-neutral-200 p-3 rounded-xl font-bold flex items-center justify-center gap-2 transition-colors"
                    >
                        <Check size={18} /> Save Changes
                    </button>
                </div>
            </div>
        </div>
    );
};

export default EditLogModal;
