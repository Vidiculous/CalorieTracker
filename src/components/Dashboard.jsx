import React, { useState, useMemo } from 'react';
import { useAppContext } from '../context/AppContext';
import ProgressRing from './ProgressRing';
import { Zap, Activity, ChevronLeft, ChevronRight, Plus, ChevronDown, ChevronUp, Pencil, Coffee, Sun, Moon, Cookie, Scale, Utensils, Calendar, Check } from 'lucide-react';

const getMealType = (hour) => {
    if (hour >= 5 && hour < 11) return 'Breakfast';
    if (hour >= 11 && hour < 16) return 'Lunch';
    if (hour >= 16 && hour < 22) return 'Dinner';
    return 'Snacks';
};

const MEAL_ORDER = ['Breakfast', 'Lunch', 'Dinner', 'Snacks'];

const LogItem = ({ log, onEditLog }) => {
    const [expanded, setExpanded] = useState(false);
    const hasSubItems = log.items && log.items.length > 0;

    return (
        <div className="mb-2">
            <div
                onClick={() => {
                    if (hasSubItems) {
                        setExpanded(!expanded);
                    } else {
                        onEditLog(log);
                    }
                }}
                className={`bg-neutral-900/40 p-3 rounded-2xl border border-neutral-800 hover:border-neutral-700 flex justify-between items-center transition-all hover:bg-neutral-800/60 cursor-pointer ${!hasSubItems ? 'active:scale-[0.98]' : ''}`}
            >
                <div className="flex-1">
                    <div className="flex justify-between items-center">
                        <div>
                            <p className="text-white font-medium text-sm">{log.food_name}</p>
                            <p className="text-neutral-500 text-xs">{log.quantity}</p>
                        </div>
                        <div className="text-right mr-2 flex items-center gap-3">
                            <div>
                                <span className="text-white font-bold text-sm block">{log.calories} kcal</span>
                                <div className="flex gap-1 justify-end text-[10px] text-neutral-400">
                                    <span className="text-blue-400">P:{log.protein}</span>
                                    <span className="text-amber-400">C:{log.carbs || 0}</span>
                                    <span className="text-rose-400">F:{log.fat || 0}</span>
                                </div>
                            </div>
                            {hasSubItems && (
                                <div className="flex items-center gap-1">
                                    <button
                                        onClick={(e) => { e.stopPropagation(); onEditLog(log); }}
                                        className="p-1.5 text-neutral-500 hover:text-white hover:bg-neutral-700 rounded-full transition-colors"
                                    >
                                        <Pencil size={14} />
                                    </button>
                                    <div className="text-neutral-500">
                                        {expanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                                    </div>
                                </div>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {hasSubItems && expanded && (
                <div className="ml-6 mt-2 space-y-2 border-l-2 border-neutral-800 pl-3 animate-in fade-in slide-in-from-top-2 duration-300">
                    {log.items.map((item, idx) => (
                        <div
                            key={item.id || idx}
                            onClick={() => onEditLog({ ...item, parentId: log.id })}
                            className="bg-neutral-900/50 p-2 rounded-xl border border-neutral-800 flex justify-between items-center cursor-pointer hover:bg-neutral-800"
                        >
                            <div>
                                <p className="text-neutral-300 font-medium text-xs">{item.food_name}</p>
                                <p className="text-neutral-600 text-[10px]">{item.quantity_desc || item.quantity}</p>
                            </div>
                            <div className="text-right">
                                <span className="text-neutral-400 font-bold text-xs block">{item.calories} kcal</span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

const MacroSuggestion = ({ calories, protein, carbs, fat, settings }) => {
    const calsLeft = (settings.dailyGoal || 2500) - calories;
    const proteinLeft = (settings.proteinGoal || 150) - protein;
    const carbsLeft = (settings.carbsGoal || 250) - carbs;
    const fatLeft = (settings.fatGoal || 70) - fat;

    const fmt = (val, unit) =>
        val > 0 ? `${Math.round(val)}${unit} short` : 'on track';

    return (
        <div className="bg-neutral-800/50 border border-neutral-700/50 rounded-2xl p-4 w-full">
            <p className="text-neutral-500 text-[10px] font-bold uppercase tracking-wider mb-2">Remaining Today</p>
            <div className="flex flex-wrap gap-x-3 gap-y-1 text-sm">
                <span className={`font-bold ${calsLeft < 0 ? 'text-red-400' : 'text-white'}`}>
                    {Math.abs(Math.round(calsLeft))} kcal {calsLeft < 0 ? 'over' : 'left'}
                </span>
                <span className="text-neutral-700">·</span>
                <span className="text-blue-400">Protein <span className="font-bold">{fmt(proteinLeft, 'g')}</span></span>
                <span className="text-neutral-700">·</span>
                <span className="text-amber-400">Carbs <span className="font-bold">{fmt(carbsLeft, 'g')}</span></span>
                <span className="text-neutral-700">·</span>
                <span className="text-rose-400">Fat <span className="font-bold">{fmt(fatLeft, 'g')}</span></span>
            </div>
        </div>
    );
};

const Dashboard = ({ onOpenAnalytics, onEditLog, onOpenManual, onOpenMealPlanner }) => {
    const { settings, getTotalsForDate, logs, currentDate, changeDate, logWeight, weightLogs, mealPlans, logPlannedMeal } = useAppContext();
    const { calories, protein, carbs, fat } = getTotalsForDate(currentDate);
    const { dailyGoal } = settings;

    const [showWeightInput, setShowWeightInput] = useState(false);
    const [weightInput, setWeightInput] = useState('');
    const [showPlanned, setShowPlanned] = useState(false);

    const remaining = dailyGoal - calories;
    const isOver = remaining < 0;
    const isToday = new Date(currentDate).toDateString() === new Date().toDateString();

    const todaysWeightLog = weightLogs.find(l => new Date(l.date).toDateString() === new Date(currentDate).toDateString());
    const displayWeight = todaysWeightLog ? todaysWeightLog.weight : null;

    // Streak counter: consecutive days with ≥1 log going back from today
    const streak = useMemo(() => {
        let count = 0;
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        for (let i = 0; i < 365; i++) {
            const checkDate = new Date(today);
            checkDate.setDate(today.getDate() - i);
            const dateStr = checkDate.toDateString();
            if (logs.some(log => new Date(log.timestamp).toDateString() === dateStr)) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }, [logs]);

    // Group logs by meal type
    const groupedLogs = useMemo(() => {
        const daysLogs = logs.filter(log => new Date(log.timestamp).toDateString() === new Date(currentDate).toDateString());

        const groups = { Breakfast: [], Lunch: [], Dinner: [], Snacks: [] };

        daysLogs.forEach(log => {
            const date = new Date(log.timestamp);
            const hour = date.getHours();
            const type = log.meal_type || getMealType(hour);
            if (groups[type]) {
                groups[type].push(log);
            } else {
                groups['Snacks'].push(log);
            }
        });

        return groups;
    }, [logs, currentDate]);

    // Planned meals for current date
    const currentDateKey = new Date(currentDate).toDateString();
    const plannedDay = mealPlans[currentDateKey] || {};
    const hasPlanned = MEAL_ORDER.some(meal => (plannedDay[meal] || []).length > 0);

    const handlePrevDay = () => {
        const d = new Date(currentDate);
        d.setDate(d.getDate() - 1);
        changeDate(d.toISOString());
    };

    const handleNextDay = () => {
        const d = new Date(currentDate);
        d.setDate(d.getDate() + 1);
        changeDate(d.toISOString());
    };

    const handleWeightSave = () => {
        if (!weightInput) return;
        logWeight(parseFloat(weightInput), currentDate);
        setShowWeightInput(false);
        setWeightInput('');
    };

    const getMealIcon = (type) => {
        switch (type) {
            case 'Breakfast': return <Coffee size={14} className="text-amber-400" />;
            case 'Lunch': return <Sun size={14} className="text-orange-400" />;
            case 'Dinner': return <Moon size={14} className="text-indigo-400" />;
            default: return <Cookie size={14} className="text-pink-400" />;
        }
    };

    return (
        <div className="flex flex-col items-center space-y-6 animate-in fade-in duration-500">

            {/* Date Navigator + Streak */}
            <div className="flex flex-col items-center gap-2 mt-4 w-full">
                <div className="flex items-center gap-4 bg-neutral-800/50 p-2 rounded-2xl border border-neutral-700/50">
                    <button onClick={handlePrevDay} className="p-2 text-neutral-400 hover:text-white transition-colors">
                        <ChevronLeft size={20} />
                    </button>
                    <div className="flex flex-col items-center min-w-[120px]">
                        <span className="text-white font-bold text-sm">
                            {new Date(currentDate).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' })}
                        </span>
                        <span className="text-[10px] text-neutral-500 font-medium uppercase tracking-wider">
                            {isToday ? 'Today' : 'History'}
                        </span>
                    </div>
                    <button
                        onClick={handleNextDay}
                        disabled={isToday}
                        className={`p-2 transition-colors ${isToday ? 'text-neutral-700 cursor-not-allowed' : 'text-neutral-400 hover:text-white'}`}
                    >
                        <ChevronRight size={20} />
                    </button>
                </div>

                {streak > 0 && (
                    <div className="flex items-center gap-1.5 bg-orange-500/15 border border-orange-500/30 px-3 py-1 rounded-full">
                        <span className="text-sm">🔥</span>
                        <span className="text-orange-400 text-xs font-bold">{streak} day streak</span>
                    </div>
                )}
            </div>

            {/* Progress Ring */}
            <div className="relative">
                <ProgressRing radius={120} stroke={12} progress={calories} goal={dailyGoal}>
                    <div className="text-center">
                        <span className="text-4xl font-bold text-white block">{calories}</span>
                        <span className="text-sm text-neutral-400 font-medium">/ {dailyGoal} kcal</span>
                    </div>
                </ProgressRing>

                <div className="absolute -bottom-4 left-1/2 -translate-x-1/2 whitespace-nowrap">
                    {isOver ? (
                        <span className="bg-red-500/20 text-red-400 px-3 py-1 rounded-full text-xs font-bold border border-red-500/50">
                            {Math.abs(remaining)} kcal Over Limit!
                        </span>
                    ) : (
                        <span className="bg-emerald-500/20 text-emerald-400 px-3 py-1 rounded-full text-xs font-bold border border-emerald-500/50">
                            {remaining} kcal left
                        </span>
                    )}
                </div>
            </div>

            {/* Macros Grid */}
            <div className="grid grid-cols-3 gap-3 w-full">
                <div className="bg-neutral-800/50 p-3 rounded-2xl border border-neutral-700/50 flex flex-col items-center gap-1">
                    <span className="text-neutral-400 text-xs font-medium">Protein</span>
                    <span className="text-lg font-bold text-white">{protein}g</span>
                    <div className="h-1 w-full bg-neutral-700 rounded-full mt-1 overflow-hidden">
                        <div className="h-full bg-blue-500" style={{ width: `${Math.min(100, (protein / (settings.proteinGoal || 150)) * 100)}%` }}></div>
                    </div>
                </div>
                <div className="bg-neutral-800/50 p-3 rounded-2xl border border-neutral-700/50 flex flex-col items-center gap-1">
                    <span className="text-neutral-400 text-xs font-medium">Carbs</span>
                    <span className="text-lg font-bold text-white">{carbs}g</span>
                    <div className="h-1 w-full bg-neutral-700 rounded-full mt-1 overflow-hidden">
                        <div className="h-full bg-amber-500" style={{ width: `${Math.min(100, (carbs / (settings.carbsGoal || 250)) * 100)}%` }}></div>
                    </div>
                </div>
                <div className="bg-neutral-800/50 p-3 rounded-2xl border border-neutral-700/50 flex flex-col items-center gap-1">
                    <span className="text-neutral-400 text-xs font-medium">Fat</span>
                    <span className="text-lg font-bold text-white">{fat}g</span>
                    <div className="h-1 w-full bg-neutral-700 rounded-full mt-1 overflow-hidden">
                        <div className="h-full bg-rose-500" style={{ width: `${Math.min(100, (fat / (settings.fatGoal || 70)) * 100)}%` }}></div>
                    </div>
                </div>
            </div>

            {/* Macro Suggestion card — only when something has been logged */}
            {calories > 0 && (
                <MacroSuggestion
                    calories={calories}
                    protein={protein}
                    carbs={carbs}
                    fat={fat}
                    settings={settings}
                />
            )}

            {/* Actions Grid */}
            <div className="grid grid-cols-4 gap-3 w-full">
                <div
                    onClick={onOpenManual}
                    className="bg-neutral-800/50 p-3 rounded-2xl border border-neutral-700/50 flex flex-col items-center justify-center gap-1.5 cursor-pointer hover:bg-neutral-700/50 transition-colors h-20"
                >
                    <div className="p-1.5 bg-emerald-500/20 rounded-full text-emerald-400">
                        <Utensils size={18} />
                    </div>
                    <span className="text-[11px] font-bold text-neutral-300 text-center leading-tight">Food</span>
                </div>

                <div
                    onClick={() => { setWeightInput(displayWeight || ''); setShowWeightInput(true); }}
                    className="bg-neutral-800/50 p-3 rounded-2xl border border-neutral-700/50 flex flex-col items-center justify-center gap-1.5 cursor-pointer hover:bg-neutral-700/50 transition-colors h-20"
                >
                    <div className="p-1.5 bg-purple-500/20 rounded-full text-purple-400">
                        <Scale size={18} />
                    </div>
                    <span className="text-[11px] font-bold text-neutral-300 text-center leading-tight">
                        {displayWeight ? `${displayWeight}kg` : 'Weight'}
                    </span>
                </div>

                <div
                    onClick={onOpenAnalytics}
                    className="bg-neutral-800/50 p-3 rounded-2xl border border-neutral-700/50 flex flex-col items-center justify-center gap-1.5 cursor-pointer hover:bg-neutral-700/50 transition-colors h-20"
                >
                    <div className="p-1.5 bg-amber-500/20 rounded-full text-amber-400">
                        <Activity size={18} />
                    </div>
                    <span className="text-[11px] font-bold text-neutral-300">Stats</span>
                </div>

                <div
                    onClick={onOpenMealPlanner}
                    className="bg-neutral-800/50 p-3 rounded-2xl border border-neutral-700/50 flex flex-col items-center justify-center gap-1.5 cursor-pointer hover:bg-neutral-700/50 transition-colors h-20"
                >
                    <div className="p-1.5 bg-indigo-500/20 rounded-full text-indigo-400">
                        <Calendar size={18} />
                    </div>
                    <span className="text-[11px] font-bold text-neutral-300">Plan</span>
                </div>
            </div>

            {/* Recent Logs List - Grouped */}
            <div className="w-full space-y-4 pb-4">
                <div className="flex justify-between items-center px-1">
                    <h3 className="text-neutral-400 text-sm font-bold">Logs</h3>
                </div>

                {Object.values(groupedLogs).every(g => g.length === 0) ? (
                    <div className="text-center text-neutral-500 text-xs py-8 bg-neutral-800/30 rounded-2xl border border-neutral-800 border-dashed">
                        No food logged for this day.
                    </div>
                ) : (
                    <div className="space-y-4">
                        {MEAL_ORDER.map(mealType => {
                            const logsForMeal = groupedLogs[mealType];
                            if (logsForMeal.length === 0) return null;

                            const mealTotals = logsForMeal.reduce((acc, item) => ({
                                calories: acc.calories + (item.calories || 0),
                                protein: acc.protein + (item.protein || 0),
                                carbs: acc.carbs + (item.carbs || 0),
                                fat: acc.fat + (item.fat || 0)
                            }), { calories: 0, protein: 0, carbs: 0, fat: 0 });

                            return (
                                <div key={mealType}>
                                    <div className="flex justify-between items-end px-2 mb-2">
                                        <div className="flex items-center gap-2 text-neutral-300 font-bold text-xs uppercase tracking-wider">
                                            {getMealIcon(mealType)}
                                            {mealType}
                                        </div>
                                        <div className="flex items-center gap-2 text-[10px] text-neutral-500 font-medium">
                                            <span className="text-white font-bold">{mealTotals.calories} kcal</span>
                                            <span className="w-[1px] h-3 bg-neutral-700"></span>
                                            <span className="text-blue-400">{mealTotals.protein}p</span>
                                            <span className="text-amber-400">{mealTotals.carbs}c</span>
                                            <span className="text-rose-400">{mealTotals.fat}f</span>
                                        </div>
                                    </div>
                                    <div className="space-y-2">
                                        {logsForMeal.map(log => (
                                            <LogItem key={log.id} log={log} onEditLog={onEditLog} />
                                        ))}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>

            {/* Planned Meals Section */}
            {hasPlanned && (
                <div className="w-full pb-4">
                    <button
                        onClick={() => setShowPlanned(p => !p)}
                        className="flex items-center justify-between w-full px-1 mb-3"
                    >
                        <div className="flex items-center gap-2">
                            <Calendar size={14} className="text-indigo-400" />
                            <h3 className="text-neutral-400 text-sm font-bold">Planned Meals</h3>
                        </div>
                        {showPlanned ? <ChevronUp size={16} className="text-neutral-500" /> : <ChevronDown size={16} className="text-neutral-500" />}
                    </button>

                    {showPlanned && (
                        <div className="space-y-3 animate-in fade-in slide-in-from-top-2 duration-200">
                            {MEAL_ORDER.map(mealType => {
                                const items = plannedDay[mealType] || [];
                                if (items.length === 0) return null;
                                const mealKcal = items.reduce((sum, i) => sum + (i.calories || 0), 0);

                                return (
                                    <div key={mealType} className="bg-neutral-800/40 border border-neutral-700/50 border-dashed rounded-2xl p-3">
                                        <div className="flex justify-between items-center mb-2">
                                            <div className="flex items-center gap-2">
                                                {getMealIcon(mealType)}
                                                <span className="text-neutral-300 text-xs font-bold uppercase tracking-wider">{mealType}</span>
                                                <span className="text-neutral-600 text-xs">{mealKcal} kcal</span>
                                            </div>
                                            <button
                                                onClick={() => logPlannedMeal(currentDateKey, mealType)}
                                                className="flex items-center gap-1 bg-indigo-500/20 hover:bg-indigo-500/30 text-indigo-400 text-xs font-bold px-2.5 py-1 rounded-lg transition-colors"
                                            >
                                                <Check size={11} />
                                                Log Meal
                                            </button>
                                        </div>
                                        <div className="space-y-1">
                                            {items.map(item => (
                                                <div key={item.id} className="flex justify-between text-xs text-neutral-500">
                                                    <span>{item.food_name}</span>
                                                    <span>{item.calories} kcal</span>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    )}
                </div>
            )}

            {/* Weight Input Modal */}
            {showWeightInput && (
                <div className="fixed inset-0 z-[70] bg-black/80 backdrop-blur-sm flex items-center justify-center p-4" onClick={e => { if (e.target === e.currentTarget) setShowWeightInput(false); }}>
                    <div className="bg-neutral-900 border border-neutral-800 p-6 rounded-3xl w-full max-w-xs animate-in zoom-in-95 duration-200 shadow-2xl">
                        <div className="flex justify-between items-center mb-4">
                            <h2 className="text-xl font-bold text-white">Log Weight</h2>
                            <button onClick={() => setShowWeightInput(false)} className="text-neutral-500 hover:text-white">
                                <Plus className="rotate-45" />
                            </button>
                        </div>

                        <div className="mb-6 text-center">
                            <p className="text-neutral-400 text-sm mb-4">Weight for {new Date(currentDate).toLocaleDateString()}</p>
                            <div className="flex items-center justify-center gap-2">
                                <input
                                    type="number"
                                    value={weightInput}
                                    onChange={e => setWeightInput(e.target.value)}
                                    placeholder="0.0"
                                    autoFocus
                                    className="bg-transparent text-white text-5xl font-bold text-center w-32 border-b-2 border-neutral-700 focus:border-purple-500 outline-none placeholder:text-neutral-800"
                                />
                                <span className="text-neutral-500 font-medium mt-4">kg</span>
                            </div>
                        </div>

                        <button
                            onClick={handleWeightSave}
                            className="w-full bg-purple-600 hover:bg-purple-700 text-white p-4 rounded-2xl font-bold text-lg transition-all active:scale-95"
                        >
                            Save Scale Entry
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Dashboard;
