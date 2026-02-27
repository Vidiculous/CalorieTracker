import React, { useState } from 'react';
import { X, Check, Trash2 } from 'lucide-react';

const confidenceBadge = (confidence) => {
    if (confidence === 'high') return { emoji: '🟢', label: 'High', cls: 'text-green-400' };
    if (confidence === 'medium') return { emoji: '🟡', label: 'Medium', cls: 'text-yellow-400' };
    return { emoji: '🔴', label: 'Low', cls: 'text-rose-400' };
};

const ConfirmLogModal = ({ items, mealType, mealName, onConfirm, onCancel }) => {
    const [editedItems, setEditedItems] = useState(
        items.map(item => ({ ...item }))
    );

    const update = (index, field, value) => {
        setEditedItems(prev => {
            const next = [...prev];
            next[index] = { ...next[index], [field]: value === '' ? '' : Number(value) };
            return next;
        });
    };

    const remove = (index) => {
        setEditedItems(prev => prev.filter((_, i) => i !== index));
    };

    const handleConfirm = () => {
        const valid = editedItems.filter(i => i.calories > 0);
        if (valid.length > 0) onConfirm(valid);
        else onCancel();
    };

    const totalCalories = editedItems.reduce((s, i) => s + (Number(i.calories) || 0), 0);

    return (
        <div className="fixed inset-0 z-[90] bg-black/70 backdrop-blur-sm flex items-end justify-center p-0">
            <div className="w-full max-w-md bg-neutral-900 border border-neutral-700 rounded-t-3xl shadow-2xl animate-in slide-in-from-bottom-4 duration-200 max-h-[85vh] flex flex-col">
                {/* Header */}
                <div className="flex items-center justify-between px-5 pt-5 pb-3 border-b border-neutral-800 flex-shrink-0">
                    <div>
                        <h2 className="text-lg font-bold text-white">Review Detected Items</h2>
                        {mealName && <p className="text-xs text-neutral-400 mt-0.5">{mealName}</p>}
                    </div>
                    <button onClick={onCancel} className="p-2 hover:bg-neutral-800 rounded-full text-neutral-400 hover:text-white transition-colors">
                        <X size={18} />
                    </button>
                </div>

                <p className="px-5 py-2 text-xs text-neutral-500 flex-shrink-0">
                    Edit values before logging. Items with 🔴 low confidence may need adjustment.
                </p>

                {/* Item list */}
                <div className="overflow-y-auto flex-1 px-4 pb-2 space-y-3">
                    {editedItems.map((item, idx) => {
                        const badge = confidenceBadge(item.confidence);
                        return (
                            <div key={idx} className="bg-neutral-800 rounded-2xl p-3 border border-neutral-700/60">
                                <div className="flex items-start justify-between mb-2 gap-2">
                                    <div className="flex-1 min-w-0">
                                        <p className="text-sm font-semibold text-white truncate">{item.food_name}</p>
                                        {item.quantity_desc && (
                                            <p className="text-xs text-neutral-500">{item.quantity_desc}</p>
                                        )}
                                    </div>
                                    <div className="flex items-center gap-2 flex-shrink-0">
                                        <span className={`text-xs font-medium ${badge.cls}`} title={`Confidence: ${badge.label}`}>
                                            {badge.emoji} {badge.label}
                                        </span>
                                        <button
                                            onClick={() => remove(idx)}
                                            className="p-1 hover:bg-neutral-700 rounded-lg text-neutral-500 hover:text-rose-400 transition-colors"
                                        >
                                            <Trash2 size={14} />
                                        </button>
                                    </div>
                                </div>
                                <div className="grid grid-cols-4 gap-2">
                                    {['calories', 'protein', 'carbs', 'fat'].map(field => (
                                        <div key={field}>
                                            <label className="text-[10px] text-neutral-500 uppercase font-bold block mb-1">
                                                {field === 'calories' ? 'kcal' : field}
                                            </label>
                                            <input
                                                type="number"
                                                min="0"
                                                value={editedItems[idx][field] ?? ''}
                                                onChange={e => update(idx, field, e.target.value)}
                                                className="w-full bg-neutral-900 border border-neutral-600 rounded-lg p-1.5 text-white text-xs text-center focus:border-rose-500 focus:outline-none"
                                            />
                                        </div>
                                    ))}
                                </div>
                            </div>
                        );
                    })}
                </div>

                {/* Footer */}
                <div className="px-4 pt-3 pb-5 border-t border-neutral-800 flex-shrink-0">
                    <div className="flex items-center justify-between mb-3">
                        <span className="text-sm text-neutral-400">
                            {editedItems.length} item{editedItems.length !== 1 ? 's' : ''}
                            {mealType && <span className="text-neutral-600"> · {mealType}</span>}
                        </span>
                        <span className="text-sm font-bold text-white">{totalCalories} kcal total</span>
                    </div>
                    <div className="flex gap-3">
                        <button
                            onClick={onCancel}
                            className="flex-1 py-3 bg-neutral-800 hover:bg-neutral-700 text-neutral-300 rounded-2xl font-semibold transition-colors text-sm"
                        >
                            Discard
                        </button>
                        <button
                            onClick={handleConfirm}
                            disabled={editedItems.length === 0}
                            className="flex-1 py-3 bg-rose-500 hover:bg-rose-600 disabled:opacity-40 disabled:cursor-not-allowed text-white rounded-2xl font-semibold transition-colors flex items-center justify-center gap-2 text-sm"
                        >
                            <Check size={16} />
                            Log All
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ConfirmLogModal;
