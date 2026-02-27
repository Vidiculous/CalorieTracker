import React, { useState, useMemo } from 'react';
import { X, Plus, Trash2, Check, Calendar } from 'lucide-react';
import { useAppContext } from '../context/AppContext';
import FoodSearch from './FoodSearch';

const MEAL_TYPES = ['Breakfast', 'Lunch', 'Dinner', 'Snacks'];

const MealPlanner = ({ onClose }) => {
    const { mealPlans, addMealPlanItem, removeMealPlanItem, logPlannedMeal } = useAppContext();
    const [selectedDate, setSelectedDate] = useState(() => {
        const d = new Date();
        d.setHours(0, 0, 0, 0);
        return d;
    });
    const [addingTo, setAddingTo] = useState(null); // { dateKey, mealType }

    // 7-day strip: today + next 6
    const days = useMemo(() => {
        return Array.from({ length: 7 }, (_, i) => {
            const d = new Date();
            d.setHours(0, 0, 0, 0);
            d.setDate(d.getDate() + i);
            return d;
        });
    }, []);

    const dateKey = selectedDate.toDateString();
    const dayPlan = mealPlans[dateKey] || {};

    const plannedKcalForDay = (date) => {
        const plan = mealPlans[date.toDateString()] || {};
        return MEAL_TYPES.reduce((sum, meal) => {
            return sum + (plan[meal] || []).reduce((s, item) => s + (item.calories || 0), 0);
        }, 0);
    };

    const handleAddFood = (item) => {
        if (!addingTo) return;
        addMealPlanItem(addingTo.dateKey, addingTo.mealType, item);
        setAddingTo(null);
    };

    return (
        <div className="fixed inset-0 z-[70] bg-neutral-900 flex flex-col animate-in slide-in-from-bottom-10 duration-300">
            {/* Header */}
            <div className="flex items-center justify-between p-4 border-b border-neutral-800 shrink-0">
                <h2 className="text-xl font-bold text-white flex items-center gap-2">
                    <Calendar size={20} className="text-indigo-400" />
                    Meal Planner
                </h2>
                <button onClick={onClose} className="p-2 text-neutral-400 hover:text-white bg-neutral-800 rounded-xl transition-colors">
                    <X size={20} />
                </button>
            </div>

            {/* 7-day strip */}
            <div className="flex gap-2 px-4 py-3 overflow-x-auto border-b border-neutral-800 shrink-0">
                {days.map((day, i) => {
                    const dk = day.toDateString();
                    const kcal = plannedKcalForDay(day);
                    const isSelected = dk === dateKey;
                    return (
                        <button
                            key={dk}
                            onClick={() => setSelectedDate(day)}
                            className={`flex flex-col items-center p-2.5 rounded-2xl min-w-[54px] transition-all border ${isSelected ? 'bg-indigo-500/20 border-indigo-500/50' : 'bg-neutral-800 border-neutral-700 hover:border-neutral-600'}`}
                        >
                            <span className={`text-[10px] font-bold uppercase ${isSelected ? 'text-indigo-400' : 'text-neutral-500'}`}>
                                {i === 0 ? 'Today' : day.toLocaleDateString('en-US', { weekday: 'short' })}
                            </span>
                            <span className={`text-sm font-bold ${isSelected ? 'text-white' : 'text-neutral-300'}`}>{day.getDate()}</span>
                            {kcal > 0 ? (
                                <span className="text-[9px] text-amber-400 font-medium mt-0.5">{kcal}</span>
                            ) : (
                                <span className="text-[9px] text-neutral-700 mt-0.5">—</span>
                            )}
                        </button>
                    );
                })}
            </div>

            {/* Meal slots */}
            <div className="flex-1 overflow-y-auto p-4 space-y-3 max-w-md w-full mx-auto">
                <p className="text-neutral-500 text-xs text-center">
                    {selectedDate.toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}
                </p>
                {MEAL_TYPES.map(mealType => {
                    const items = (dayPlan[mealType] || []);
                    const mealKcal = items.reduce((sum, item) => sum + (item.calories || 0), 0);

                    return (
                        <div key={mealType} className="bg-neutral-800 border border-neutral-700 rounded-2xl p-4">
                            <div className="flex justify-between items-center mb-3">
                                <div>
                                    <h3 className="text-white font-bold text-sm">{mealType}</h3>
                                    {mealKcal > 0 && (
                                        <p className="text-neutral-500 text-xs">{mealKcal} kcal planned</p>
                                    )}
                                </div>
                                {items.length > 0 && (
                                    <button
                                        onClick={() => logPlannedMeal(dateKey, mealType)}
                                        className="flex items-center gap-1.5 bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-400 text-xs font-bold px-3 py-1.5 rounded-xl transition-colors active:scale-95"
                                    >
                                        <Check size={12} />
                                        Log Now
                                    </button>
                                )}
                            </div>

                            {items.length > 0 && (
                                <div className="space-y-2 mb-3">
                                    {items.map(item => (
                                        <div key={item.id} className="flex justify-between items-center bg-neutral-900 p-2.5 rounded-xl">
                                            <div className="flex-1 min-w-0 mr-2">
                                                <p className="text-white text-xs font-medium truncate">{item.food_name}</p>
                                                {item.quantity && <p className="text-neutral-600 text-[10px]">{item.quantity}</p>}
                                            </div>
                                            <div className="flex items-center gap-2 shrink-0">
                                                <span className="text-neutral-400 text-xs">{item.calories} kcal</span>
                                                <button
                                                    onClick={() => removeMealPlanItem(dateKey, mealType, item.id)}
                                                    className="p-1 text-neutral-600 hover:text-rose-400 rounded-lg transition-colors"
                                                >
                                                    <Trash2 size={12} />
                                                </button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}

                            <button
                                onClick={() => setAddingTo({ dateKey, mealType })}
                                className="w-full flex items-center justify-center gap-2 text-neutral-500 hover:text-white border border-dashed border-neutral-700 hover:border-neutral-600 rounded-xl py-2 text-xs transition-colors"
                            >
                                <Plus size={13} />
                                Add Item
                            </button>
                        </div>
                    );
                })}
            </div>

            {/* FoodSearch modal when adding */}
            {addingTo && (
                <FoodSearch
                    onClose={() => setAddingTo(null)}
                    onAdd={handleAddFood}
                />
            )}
        </div>
    );
};

export default MealPlanner;
