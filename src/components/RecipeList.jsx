import React, { useState } from 'react';
import { useAppContext } from '../context/AppContext';
import { Plus, Trash2, X, ChevronDown, ChevronUp, Minus, Pencil, Check, Clock, Coffee, Sun, Moon, Cookie, LayoutGrid } from 'lucide-react';

const MealSelector = ({ value, onChange }) => {
    const [isOpen, setIsOpen] = useState(false);

    const OPTIONS = [
        { id: '', label: 'Auto', icon: Clock, color: 'text-neutral-400' },
        { id: 'Breakfast', label: 'Breakfast', icon: Coffee, color: 'text-amber-500' },
        { id: 'Lunch', label: 'Lunch', icon: Sun, color: 'text-orange-500' },
        { id: 'Dinner', label: 'Dinner', icon: Moon, color: 'text-indigo-400' },
        { id: 'Snacks', label: 'Snacks', icon: Cookie, color: 'text-rose-400' },
    ];

    const currentOption = OPTIONS.find(o => o.id === value) || OPTIONS[0];
    const Icon = currentOption.icon;

    return (
        <div className="relative">
            <button
                onClick={(e) => { e.stopPropagation(); setIsOpen(!isOpen); }}
                className={`flex items-center gap-2 bg-neutral-900 border ${isOpen ? 'border-neutral-600' : 'border-neutral-700'} hover:border-neutral-600 rounded-lg p-1.5 transition-all w-8 h-8 justify-center`}
                title={`Meal Section: ${currentOption.label}`}
            >
                <Icon size={14} className={currentOption.color} />
            </button>

            {isOpen && (
                <>
                    <div className="fixed inset-0 z-[60]" onClick={(e) => { e.stopPropagation(); setIsOpen(false); }} />
                    <div className="absolute top-full right-0 mt-2 w-32 bg-neutral-900 border border-neutral-700 rounded-xl shadow-xl z-[70] overflow-hidden animate-in fade-in slide-in-from-top-1 duration-200">
                        <div className="py-1">
                            {OPTIONS.map((opt) => (
                                <button
                                    key={opt.id}
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        onChange(opt.id);
                                        setIsOpen(false);
                                    }}
                                    className={`w-full text-left px-3 py-2 text-[11px] font-medium transition-colors flex items-center gap-3 ${currentOption.id === opt.id
                                        ? 'bg-white/5 text-white'
                                        : 'text-neutral-400 hover:bg-white/5 hover:text-neutral-200'
                                        }`}
                                >
                                    <opt.icon size={12} className={opt.color} />
                                    <span>{opt.label}</span>
                                    {currentOption.id === opt.id && <Check size={10} className="ml-auto" />}
                                </button>
                            ))}
                        </div>
                    </div>
                </>
            )}
        </div>
    );
};

const RecipeItem = ({ recipe, onSelect, onDelete, onUpdate }) => {
    const [expanded, setExpanded] = useState(false);
    const [portions, setPortions] = useState(1);
    const [mealType, setMealType] = useState(''); // '' = Auto/Time-based

    // Editing state
    const [isEditing, setIsEditing] = useState(false);
    const [editServings, setEditServings] = useState(recipe.servings || 1);

    const hasItems = recipe.items && recipe.items.length > 0;
    const totalServings = recipe.servings || 1;

    // Calculate stats
    const caloriesPerServing = Math.round(recipe.calories / totalServings);
    const scalingFactor = portions / totalServings;

    const handleLog = (e) => {
        e.stopPropagation();

        const scaledRecipe = {
            ...recipe,
            name: `${recipe.name} (${portions} serving${portions > 1 ? 's' : ''})`,
            calories: Math.round(recipe.calories * scalingFactor),
            protein: Math.round(recipe.protein * scalingFactor),
            carbs: Math.round((recipe.carbs || 0) * scalingFactor),
            fat: Math.round((recipe.fat || 0) * scalingFactor),
            quantity: `${portions} serving${portions > 1 ? 's' : ''}`,
            meal_type: mealType || undefined, // Pass selected meal type
            items: recipe.items?.map(item => ({
                ...item,
                calories: Math.round(item.calories * scalingFactor),
                protein: Math.round(item.protein * scalingFactor),
                carbs: Math.round((item.carbs || 0) * scalingFactor),
                fat: Math.round((item.fat || 0) * scalingFactor),
            }))
        };
        onSelect(scaledRecipe);
    };

    const saveServingChange = (e) => {
        e.stopPropagation();
        const newServings = parseFloat(editServings);
        if (newServings > 0) {
            onUpdate(recipe.id, { servings: newServings });
            setIsEditing(false);
        }
    };

    return (
        <div className="mb-3">
            {/* Main Recipe Card */}
            <div
                className={`bg-neutral-800/40 p-4 rounded-2xl border border-neutral-700/50 transition-all hover:bg-neutral-800/60 cursor-pointer ${!hasItems ? 'active:scale-[0.98]' : ''}`}
                onClick={() => hasItems && setExpanded(!expanded)}
            >
                <div className="flex flex-col gap-4">
                    {/* Top Row: Title & Batch Info */}
                    <div className="flex justify-between items-start">
                        <div className="flex-1 min-w-0 mr-4">
                            <p className="text-white font-bold text-sm leading-tight line-clamp-2">{recipe.name}</p>

                            {/* Batch Info (Clean Text Line) */}
                            <div className="mt-1.5 flex items-center text-xs text-neutral-500 font-medium" onClick={e => e.stopPropagation()}>
                                <span>Batch: <span className="text-neutral-300">{Math.round(recipe.calories)}</span> kcal</span>
                                <span className="mx-1.5 opacity-30">|</span>

                                {isEditing ? (
                                    <div className="flex items-center gap-1 animate-in fade-in duration-200">
                                        <input
                                            type="number"
                                            value={editServings}
                                            onChange={(e) => setEditServings(e.target.value)}
                                            className="w-8 bg-neutral-900 border-b border-rose-500 text-white text-center focus:outline-none p-0 [appearance:textfield] [&::-webkit-outer-spin-button]:appearance-none [&::-webkit-inner-spin-button]:appearance-none"
                                            autoFocus
                                            onClick={e => e.stopPropagation()}
                                        />
                                        <button onClick={saveServingChange} className="text-rose-500 hover:text-rose-400 p-0.5">
                                            <Check size={14} />
                                        </button>
                                    </div>
                                ) : (
                                    <div
                                        className="flex items-center gap-1 group/edit cursor-pointer hover:text-neutral-300 transition-colors"
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            setEditServings(totalServings);
                                            setIsEditing(true);
                                        }}
                                    >
                                        <span>{totalServings} serv.</span>
                                        <Pencil size={10} className="opacity-0 group-hover/edit:opacity-100 transition-opacity" />
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Calories Badge (Current Selected) */}
                        <div className="text-right flex-shrink-0 bg-neutral-900/50 px-3 py-1.5 rounded-xl border border-neutral-800/50">
                            <span className="text-white font-bold text-sm block">
                                {Math.round(recipe.calories * scalingFactor)}
                            </span>
                            <span className="text-[10px] text-neutral-500 block">kcal</span>
                        </div>
                    </div>

                    {/* Bottom Row: Controls */}
                    <div className="flex items-center justify-between pt-2 border-t border-white/5" onClick={e => e.stopPropagation()}>

                        {/* Left: Portions */}
                        <div className="flex items-center gap-3">
                            <span className="text-[10px] text-neutral-500 font-bold uppercase tracking-wider">Log Amount</span>
                            <div className="flex items-center bg-neutral-900 rounded-lg border border-neutral-700 h-8">
                                <button
                                    onClick={() => setPortions(Math.max(0.5, portions - 0.5))}
                                    className="w-8 h-full flex items-center justify-center text-neutral-400 hover:text-white hover:bg-neutral-800 rounded-l-lg transition-colors"
                                >
                                    <Minus size={12} />
                                </button>
                                <span className="text-xs w-8 text-center font-bold text-white leading-none">{portions}</span>
                                <button
                                    onClick={() => setPortions(portions + 0.5)}
                                    className="w-8 h-full flex items-center justify-center text-neutral-400 hover:text-white hover:bg-neutral-800 rounded-r-lg transition-colors"
                                >
                                    <Plus size={12} />
                                </button>
                            </div>
                        </div>

                        {/* Right: Actions */}
                        <div className="flex items-center gap-2">
                            {/* Meal Selector */}
                            <MealSelector value={mealType} onChange={setMealType} />

                            <div className="h-4 w-[1px] bg-white/10 mx-1"></div>

                            <button
                                onClick={handleLog}
                                className="h-8 px-4 flex items-center gap-1.5 bg-rose-500 text-white rounded-lg hover:bg-rose-600 transition-all shadow-lg shadow-rose-500/20 active:scale-95 font-medium text-xs"
                            >
                                <Plus size={14} />
                                Log
                            </button>

                            {/* More Actions Group */}
                            <div className="flex items-center gap-1 ml-1">
                                <button
                                    onClick={() => onDelete(recipe.id)}
                                    className="h-8 w-8 flex items-center justify-center text-neutral-600 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
                                >
                                    <Trash2 size={14} />
                                </button>
                                {hasItems && (
                                    <div
                                        onClick={() => setExpanded(!expanded)}
                                        className={`h-8 w-8 flex items-center justify-center text-neutral-500 cursor-pointer hover:text-white transition-transform duration-300 ${expanded ? 'rotate-180' : ''}`}
                                    >
                                        <ChevronDown size={16} />
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Sub Items */}
            {hasItems && expanded && (
                <div className="ml-1 mt-1 space-y-1 animate-in fade-in slide-in-from-top-1 duration-200">
                    {recipe.description && (
                        <div className="bg-neutral-900/20 p-3 rounded-lg border border-white/5 mb-2 mx-2">
                            <p className="text-[10px] text-neutral-500 font-bold uppercase tracking-wider mb-1">Instructions</p>
                            <p className="text-xs text-neutral-400 leading-relaxed font-serif tracking-wide">{recipe.description}</p>
                        </div>
                    )}

                    {recipe.items.map((item, idx) => (
                        <div
                            key={item.id || idx}
                            className="bg-neutral-900/20 px-4 py-2 mx-2 rounded-lg flex justify-between items-center group hover:bg-neutral-800/40 transition-colors"
                        >
                            <div className="flex-1 min-w-0 pr-4">
                                <p className="text-neutral-400 font-medium text-xs group-hover:text-neutral-300 truncate transition-colors">{item.food_name}</p>
                                <p className="text-neutral-600 text-[10px] truncate">
                                    {item.quantity_desc || item.quantity || ""}
                                </p>
                            </div>
                            <div className="text-right flex-shrink-0">
                                <span className="text-neutral-500 font-mono text-xs block group-hover:text-white transition-colors">
                                    {Math.round(item.calories * scalingFactor)}
                                </span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

const RecipeList = ({ onClose, onSelect }) => {
    const { recipes, removeRecipe, updateRecipe } = useAppContext();

    return (
        <div className="fixed inset-0 z-[60] bg-black/60 backdrop-blur-md flex items-end sm:items-center justify-center sm:p-4">
            <div className="bg-neutral-950 border border-white/10 w-full max-w-md h-[85vh] sm:h-auto sm:max-h-[85vh] sm:rounded-3xl rounded-t-3xl flex flex-col shadow-2xl animate-in slide-in-from-bottom duration-300">

                <div className="p-5 border-b border-white/5 flex justify-between items-center bg-neutral-950/50 backdrop-blur-xl sticky top-0 z-10 sm:rounded-t-3xl">
                    <div className="flex items-center gap-3">
                        <div className="bg-rose-500/10 p-2 rounded-xl text-rose-500"><LayoutGrid size={20} /></div>
                        <div>
                            <h2 className="text-lg font-bold text-white">Recipe Book</h2>
                            <p className="text-xs text-neutral-500">Fast logging for saved meals</p>
                        </div>
                    </div>
                    <button onClick={onClose} className="p-2 bg-neutral-900 hover:bg-neutral-800 rounded-full text-neutral-400 hover:text-white transition-colors border border-white/5">
                        <X size={20} />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-4 space-y-2 custom-scrollbar bg-neutral-950">
                    {recipes.length === 0 ? (
                        <div className="text-center text-neutral-500 py-12 flex flex-col items-center gap-3">
                            <div className="bg-neutral-900 p-4 rounded-full mb-2 border border-neutral-800">
                                <Plus size={24} className="text-neutral-600" />
                            </div>
                            <h3 className="text-white font-medium">No recipes yet</h3>
                            <p className="text-xs text-neutral-600 max-w-[200px] leading-relaxed">
                                Ask the AI to save a recipe, or paste a URL to import one automatically.
                            </p>
                        </div>
                    ) : (
                        recipes.map(recipe => (
                            <RecipeItem
                                key={recipe.id}
                                recipe={recipe}
                                onSelect={onSelect}
                                onDelete={removeRecipe}
                                onUpdate={updateRecipe}
                            />
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};
export default RecipeList;
