import React from 'react';
import { X, Zap, Trash2 } from 'lucide-react';
import { useAppContext } from '../context/AppContext';

const MealTemplates = ({ onClose }) => {
    const { mealTemplates, removeTemplate, addLog, currentDate } = useAppContext();

    const handleLogTemplate = (template) => {
        template.items.forEach(item => {
            addLog({
                food_name: item.food_name,
                calories: item.calories,
                protein: item.protein || 0,
                carbs: item.carbs || 0,
                fat: item.fat || 0,
                quantity: item.quantity || '1 serving',
                source: 'template',
                timestamp: currentDate,
            });
        });
        onClose();
    };

    return (
        <div className="fixed inset-0 z-[70] bg-black/60 backdrop-blur-sm flex items-end sm:items-center justify-center p-0 sm:p-4" onClick={onClose}>
            <div className="bg-neutral-900 border border-neutral-800 rounded-t-3xl sm:rounded-3xl w-full sm:max-w-md max-h-[80vh] flex flex-col shadow-2xl" onClick={e => e.stopPropagation()}>

                <div className="flex items-center justify-between p-5 border-b border-neutral-800 shrink-0">
                    <h2 className="text-xl font-bold text-white flex items-center gap-2">
                        <span className="p-2 bg-amber-500/20 rounded-xl text-amber-400"><Zap size={18} /></span>
                        Meal Templates
                    </h2>
                    <button onClick={onClose} className="p-2 text-neutral-400 hover:text-white bg-neutral-800 hover:bg-neutral-700 rounded-xl transition-colors">
                        <X size={20} />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-4 space-y-3">
                    {mealTemplates.length === 0 ? (
                        <div className="text-center py-14">
                            <p className="text-4xl mb-3">⚡</p>
                            <p className="text-neutral-400 font-medium mb-1">No templates saved yet</p>
                            <p className="text-neutral-600 text-sm">Use "Save as Template" in the manual food entry form to save a meal here for one-tap logging.</p>
                        </div>
                    ) : (
                        mealTemplates.map(template => {
                            const totalCalories = template.items.reduce((sum, i) => sum + (i.calories || 0), 0);
                            const totalProtein = template.items.reduce((sum, i) => sum + (i.protein || 0), 0);
                            const totalCarbs = template.items.reduce((sum, i) => sum + (i.carbs || 0), 0);
                            const totalFat = template.items.reduce((sum, i) => sum + (i.fat || 0), 0);

                            return (
                                <div key={template.id} className="bg-neutral-800 border border-neutral-700 rounded-2xl p-4">
                                    <div className="flex justify-between items-start mb-2">
                                        <div>
                                            <p className="text-white font-bold">{template.name}</p>
                                            <p className="text-neutral-500 text-xs">{template.items.length} item{template.items.length !== 1 ? 's' : ''}</p>
                                        </div>
                                        <button
                                            onClick={() => removeTemplate(template.id)}
                                            className="p-2 text-neutral-600 hover:text-rose-400 hover:bg-rose-500/10 rounded-xl transition-colors"
                                        >
                                            <Trash2 size={15} />
                                        </button>
                                    </div>
                                    <div className="flex gap-3 text-xs mb-3">
                                        <span className="text-white font-bold">{totalCalories} kcal</span>
                                        <span className="text-blue-400">P:{totalProtein}g</span>
                                        <span className="text-amber-400">C:{totalCarbs}g</span>
                                        <span className="text-rose-400">F:{totalFat}g</span>
                                    </div>
                                    <button
                                        onClick={() => handleLogTemplate(template)}
                                        className="w-full bg-amber-500/20 hover:bg-amber-500/30 text-amber-400 font-bold py-2.5 rounded-xl text-sm transition-colors flex items-center justify-center gap-2 active:scale-[0.98]"
                                    >
                                        <Zap size={14} />
                                        Log Now
                                    </button>
                                </div>
                            );
                        })
                    )}
                </div>
            </div>
        </div>
    );
};

export default MealTemplates;
