import React, { useMemo, useState, useEffect } from 'react';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
    PieChart, Pie, Cell, ReferenceLine, AreaChart, Area, CartesianGrid, LineChart, Line
} from 'recharts';
import { useAppContext } from '../context/AppContext';
import { Trophy, TrendingUp, Flame, Utensils, Award } from 'lucide-react';

const Analytics = ({ onClose }) => {
    const { logs, settings, weightLogs } = useAppContext();

    // --- Data Processing ---

    // 1. Weekly Data (Last 7 Days)
    const weeklyData = useMemo(() => {
        const last7Days = Array.from({ length: 7 }, (_, i) => {
            const d = new Date();
            d.setDate(d.getDate() - (6 - i)); // Order Mon -> Sun
            return d.toDateString();
        });

        return last7Days.map(dateStr => {
            const dayLogs = logs.filter(l => new Date(l.timestamp).toDateString() === dateStr);
            const totals = dayLogs.reduce((acc, curr) => ({
                calories: acc.calories + (curr.calories || 0),
                protein: acc.protein + (curr.protein || 0),
                carbs: acc.carbs + (curr.carbs || 0),
                fat: acc.fat + (curr.fat || 0),
            }), { calories: 0, protein: 0, carbs: 0, fat: 0 });

            const dayName = new Date(dateStr).toLocaleDateString('en-US', { weekday: 'short' });
            return {
                name: dayName,
                date: dateStr,
                ...totals,
                isOver: totals.calories > settings.dailyGoal
            };
        });
    }, [logs, settings.dailyGoal]);

    // 2. Averages
    const averages = useMemo(() => {
        const totalCals = weeklyData.reduce((sum, d) => sum + d.calories, 0);
        const totalProt = weeklyData.reduce((sum, d) => sum + d.protein, 0);
        return {
            avgCals: Math.round(totalCals / 7),
            avgProt: Math.round(totalProt / 7)
        };
    }, [weeklyData]);

    // 3. Meal Distribution (All Time - Last 30 Days)
    const mealDistData = useMemo(() => {
        const counts = { Breakfast: 0, Lunch: 0, Dinner: 0, Snacks: 0 };
        logs.forEach(log => {
            // Simple inference if not set, or use stored type
            let type = log.meal_type;
            if (!type) {
                const h = new Date(log.timestamp).getHours();
                if (h >= 5 && h < 11) type = 'Breakfast';
                else if (h >= 11 && h < 16) type = 'Lunch';
                else if (h >= 16 && h < 22) type = 'Dinner';
                else type = 'Snacks';
            }
            if (counts[type] !== undefined) counts[type] += (log.calories || 0); // Count by Calories
        });

        return Object.keys(counts).map(key => ({
            name: key,
            value: counts[key],
            color: key === 'Breakfast' ? '#fbbf24' : key === 'Lunch' ? '#f97316' : key === 'Dinner' ? '#818cf8' : '#f43f5e'
        })).filter(d => d.value > 0);
    }, [logs]);

    // 4. Top Foods
    const topFoods = useMemo(() => {
        const counts = {};
        logs.forEach(log => {
            const name = log.food_name;
            if (!counts[name]) counts[name] = { count: 0, cals: 0 };
            counts[name].count += 1;
            counts[name].cals += log.calories;
        });
        return Object.entries(counts)
            .sort((a, b) => b[1].count - a[1].count)
            .slice(0, 5)
            .map(([name, data]) => ({ name, ...data }));
    }, [logs]);


    return (
        <div className="fixed inset-0 z-[60] bg-black/80 backdrop-blur-md flex items-end sm:items-center justify-center sm:p-4">
            <div className="bg-neutral-900 border border-neutral-800 w-full max-w-2xl h-[90vh] sm:h-auto sm:max-h-[90vh] sm:rounded-3xl rounded-t-3xl flex flex-col shadow-2xl animate-in slide-in-from-bottom duration-300">

                <div className="p-5 border-b border-neutral-800 flex justify-between items-center bg-neutral-900 sticky top-0 z-10 sm:rounded-t-3xl">
                    <div className="flex items-center gap-3">
                        <div className="bg-purple-500/10 p-2 rounded-xl text-purple-500">
                            <TrendingUp size={20} />
                        </div>
                        <div>
                            <h2 className="text-lg font-bold text-white">Insights</h2>
                            <p className="text-xs text-neutral-500">Your weekly performance</p>
                        </div>
                    </div>
                    <button onClick={onClose} className="p-2 bg-neutral-800 hover:bg-neutral-700 rounded-full text-neutral-400 hover:text-white transition-colors">
                        <XIcon />
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-5 space-y-6 custom-scrollbar">

                    {/* Key Stats Grid */}
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                        <StatCard
                            icon={<Flame size={16} />}
                            color="text-orange-500"
                            bg="bg-orange-500/10"
                            label="Avg Calories"
                            value={averages.avgCals}
                            sub={`Goal: ${settings.dailyGoal}`}
                        />
                        <StatCard
                            icon={<Trophy size={16} />}
                            color="text-blue-500"
                            bg="bg-blue-500/10"
                            label="Avg Protein"
                            value={`${averages.avgProt}g`}
                            sub={`Goal: ${settings.proteinGoal}g`}
                        />
                        <StatCard
                            icon={<Utensils size={16} />}
                            color="text-emerald-500"
                            bg="bg-emerald-500/10"
                            label="Total Logs"
                            value={logs.length}
                            sub="Lifetime"
                        />
                        <StatCard
                            icon={<Award size={16} />}
                            color="text-yellow-500"
                            bg="bg-yellow-500/10"
                            label="Best Day"
                            value={weeklyData.reduce((prev, current) => (Math.abs(current.calories - settings.dailyGoal) < Math.abs(prev.calories - settings.dailyGoal) ? current : prev)).name}
                            sub="Closest to Goal"
                        />
                    </div>

                    {/* Chart: Calories Trend */}
                    <div className="bg-neutral-950/50 p-5 rounded-3xl border border-neutral-800">
                        <div className="flex justify-between items-center mb-6">
                            <h3 className="text-sm font-bold text-white">Calorie Trend</h3>
                            <div className="flex gap-4 text-[10px] font-medium">
                                <div className="flex items-center gap-1.5 text-emerald-400"><div className="w-2 h-2 rounded-full bg-emerald-500"></div>Under Goal</div>
                                <div className="flex items-center gap-1.5 text-rose-400"><div className="w-2 h-2 rounded-full bg-rose-500"></div>Over Goal</div>
                            </div>
                        </div>
                        <div className="h-56 w-full flex items-center justify-center">
                            <SafeChart>
                                <ResponsiveContainer width="100%" height="100%">
                                    <BarChart data={weeklyData} barSize={32}>
                                        <CartesianGrid vertical={false} stroke="#262626" strokeDasharray="3 3" />
                                        <XAxis dataKey="name" stroke="#525252" fontSize={11} tickLine={false} axisLine={false} dy={10} />
                                        <YAxis hide />
                                        <Tooltip
                                            cursor={{ fill: 'transparent' }}
                                            content={({ active, payload }) => {
                                                if (active && payload && payload.length) {
                                                    const data = payload[0].payload;
                                                    return (
                                                        <div className="bg-neutral-900 border border-neutral-700 p-3 rounded-xl shadow-xl">
                                                            <p className="text-white font-bold text-xs mb-1">{data.name}</p>
                                                            <p className={`${data.isOver ? 'text-rose-400' : 'text-emerald-400'} font-bold text-lg`}>
                                                                {data.calories} <span className="text-xs text-neutral-500">kcal</span>
                                                            </p>
                                                            <p className="text-[10px] text-neutral-500 mt-1">
                                                                {data.isOver ? `+${data.calories - settings.dailyGoal} over` : `${settings.dailyGoal - data.calories} left`}
                                                            </p>
                                                        </div>
                                                    );
                                                }
                                                return null;
                                            }}
                                        />
                                        <ReferenceLine y={settings.dailyGoal} stroke="#525252" strokeDasharray="3 3" />
                                        <Bar dataKey="calories" radius={[6, 6, 6, 6]}>
                                            {weeklyData.map((entry, index) => (
                                                <Cell key={`cell-${index}`} fill={entry.calories > settings.dailyGoal ? '#f43f5e' : '#10b981'} />
                                            ))}
                                        </Bar>
                                    </BarChart>
                                </ResponsiveContainer>
                            </SafeChart>
                        </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        {/* Macro Trends */}
                        <div className="bg-neutral-950/50 p-5 rounded-3xl border border-neutral-800">
                            <h3 className="text-sm font-bold text-white mb-4">Macro Trends</h3>
                            <div className="h-40 w-full flex items-center justify-center">
                                <SafeChart>
                                    <ResponsiveContainer width="100%" height="100%">
                                        <AreaChart data={weeklyData}>
                                            <defs>
                                                <linearGradient id="colorProt" x1="0" y1="0" x2="0" y2="1">
                                                    <stop offset="5%" stopColor="#3b82f6" stopOpacity={0.3} />
                                                    <stop offset="95%" stopColor="#3b82f6" stopOpacity={0} />
                                                </linearGradient>
                                            </defs>
                                            <Tooltip contentStyle={{ backgroundColor: '#171717', border: 'none', borderRadius: '8px', fontSize: '12px' }} />
                                            <Area type="monotone" dataKey="protein" stroke="#3b82f6" fillOpacity={1} fill="url(#colorProt)" strokeWidth={2} />
                                        </AreaChart>
                                    </ResponsiveContainer>
                                </SafeChart>
                            </div>
                            <div className="text-center text-xs text-neutral-500 mt-2 font-medium">Protein Consistency</div>
                        </div>

                        {/* Top Foods */}
                        <div className="bg-neutral-950/50 p-5 rounded-3xl border border-neutral-800">
                            <h3 className="text-sm font-bold text-white mb-4">Top Favorites</h3>
                            <div className="space-y-3">
                                {topFoods.length === 0 ? <p className="text-neutral-500 text-xs">No data yet.</p> :
                                    topFoods.map((food, i) => (
                                        <div key={i} className="flex justify-between items-center text-xs">
                                            <div className="flex items-center gap-2">
                                                <span className="w-5 h-5 flex items-center justify-center bg-neutral-800 rounded text-neutral-500 font-bold text-[10px]">#{i + 1}</span>
                                                <span className="text-neutral-300 truncate max-w-[100px]">{food.name}</span>
                                            </div>
                                            <span className="text-neutral-500">{food.count}x</span>
                                        </div>
                                    ))
                                }
                            </div>
                        </div>

                        {/* Weight Trend */}
                        <div className="bg-neutral-950/50 p-5 rounded-3xl border border-neutral-800 sm:col-span-2">
                            <div className="flex justify-between items-center mb-4">
                                <h3 className="text-sm font-bold text-white">Weight History</h3>
                                <div className="text-[10px] text-neutral-500">
                                    Current: <span className="text-white font-bold">{settings.currentWeight || '--'}kg</span>
                                    <span className="mx-2">•</span>
                                    Goal: <span className="text-emerald-400 font-bold">{settings.goalWeight || '--'}kg</span>
                                </div>
                            </div>
                            <div className="h-48 w-full flex items-center justify-center">
                                <SafeChart>
                                    <ResponsiveContainer width="100%" height="100%">
                                        <LineChart data={logs.length > 0 ? Array.from(weightLogs).sort((a, b) => new Date(a.date) - new Date(b.date)) : []}>
                                            <CartesianGrid vertical={false} stroke="#262626" strokeDasharray="3 3" />
                                            <XAxis
                                                dataKey="date"
                                                stroke="#525252"
                                                fontSize={11}
                                                tickLine={false}
                                                axisLine={false}
                                                dy={10}
                                                tickFormatter={(str) => new Date(str).toLocaleDateString('en-US', { day: 'numeric', month: 'short' })}
                                            />
                                            <YAxis domain={['auto', 'auto']} hide />
                                            <Tooltip
                                                contentStyle={{ backgroundColor: '#171717', border: 'none', borderRadius: '8px', fontSize: '12px' }}
                                                labelFormatter={(label) => new Date(label).toLocaleDateString()}
                                                formatter={(value) => [`${value} kg`, 'Weight']}
                                            />
                                            {settings.goalWeight > 0 && (
                                                <ReferenceLine y={settings.goalWeight} stroke="#10b981" strokeDasharray="3 3" label={{ value: 'Goal', position: 'right', fill: '#10b981', fontSize: 10 }} />
                                            )}
                                            <Line type="monotone" dataKey="weight" stroke="#8b5cf6" strokeWidth={2} dot={{ r: 3, fill: '#8b5cf6' }} activeDot={{ r: 5 }} />
                                        </LineChart>
                                    </ResponsiveContainer>
                                </SafeChart>
                            </div>
                        </div>
                    </div>

                </div>
            </div>
        </div>
    );
};

// Wrapper to prevent Recharts "width(-1)" error by ensuring container has dimensions
const SafeChart = ({ children, height = "100%", width = "100%" }) => {
    const containerRef = React.useRef(null);
    const [dimensions, setDimensions] = useState({ width: 0, height: 0 });

    useEffect(() => {
        if (!containerRef.current) return;

        const resizeObserver = new ResizeObserver((entries) => {
            if (!Array.isArray(entries) || !entries.length) return;
            const entry = entries[0];
            if (entry.contentRect.width > 0 && entry.contentRect.height > 0) {
                setDimensions({
                    width: entry.contentRect.width,
                    height: entry.contentRect.height
                });
            }
        });

        resizeObserver.observe(containerRef.current);
        return () => resizeObserver.disconnect();
    }, []);

    return (
        <div ref={containerRef} style={{ width, height }} className="overflow-hidden">
            {dimensions.width > 0 && dimensions.height > 0 ? children : null}
        </div>
    );
};

const StatCard = ({ icon, color, bg, label, value, sub }) => (
    <div className="bg-neutral-950/50 p-4 rounded-2xl border border-neutral-800 flex flex-col gap-3">
        <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${bg} ${color}`}>
            {icon}
        </div>
        <div>
            <p className="text-neutral-500 text-[10px] font-bold uppercase tracking-wider">{label}</p>
            <p className="text-white font-bold text-xl">{value}</p>
            <p className="text-neutral-600 text-[10px]">{sub}</p>
        </div>
    </div>
);

const XIcon = () => (
    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M18 6 6 18" /><path d="m6 6 12 12" /></svg>
);

export default Analytics;
