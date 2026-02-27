
import React, { createContext, useContext, useState, useEffect } from 'react';
import { v4 as uuidv4 } from 'uuid';

const AppContext = createContext();

export const useAppContext = () => {
    const context = useContext(AppContext);
    if (!context) {
        throw new Error('useAppContext must be used within an AppProvider');
    }
    return context;
};

const DEFAULT_SETTINGS = {
    dailyGoal: 2500,
    proteinGoal: 150,
    carbsGoal: 250,
    fatGoal: 70,
    currentWeight: 0,
    goalWeight: 0,
    hasOnboarded: false,
    selectedModel: 'gemini-3-flash-preview',
    apiKey: '',
    autoSubmit: true,
};

const STORAGE_KEYS = {
    SETTINGS: 'calorie_tracker_settings',
    LOGS: 'calorie_tracker_logs',
    RECIPES: 'calorie_tracker_recipes',
    WEIGHT_LOGS: 'calorie_tracker_weight_logs',
    CHAT: 'calorie_tracker_chat',
    TEMPLATES: 'calorie_tracker_templates',
    MEAL_PLANS: 'calorie_tracker_meal_plans',
};

export const AppProvider = ({ children }) => {
    // --- State ---
    const [settings, setSettings] = useState(DEFAULT_SETTINGS);
    const [logs, setLogs] = useState([]);
    const [recipes, setRecipes] = useState([]);
    const [weightLogs, setWeightLogs] = useState([]);
    const [chatMessages, setChatMessages] = useState([
        { id: 'welcome', role: 'ai', content: 'Hello! I can help you track calories. Send me a photo of your food, type what you ate, or just say it!' }
    ]);
    const [mealTemplates, setMealTemplates] = useState([]);
    const [mealPlans, setMealPlans] = useState({});
    const [loading, setLoading] = useState(true);

    // --- Persistence (Load) ---
    useEffect(() => {
        try {
            const savedSettings = localStorage.getItem(STORAGE_KEYS.SETTINGS);
            const savedLogs = localStorage.getItem(STORAGE_KEYS.LOGS);
            const savedRecipes = localStorage.getItem(STORAGE_KEYS.RECIPES);
            const savedWeightLogs = localStorage.getItem(STORAGE_KEYS.WEIGHT_LOGS);

            if (savedSettings) {
                const parsed = JSON.parse(savedSettings);
                // Migration: Force upgrade from retired models
                if (parsed.selectedModel === 'gemini-1.5-flash') {
                    parsed.selectedModel = 'gemini-2.0-flash-exp';
                }
                setSettings({ ...DEFAULT_SETTINGS, ...parsed });
            }
            if (savedLogs) setLogs(JSON.parse(savedLogs));
            if (savedRecipes) setRecipes(JSON.parse(savedRecipes));
            if (savedWeightLogs) setWeightLogs(JSON.parse(savedWeightLogs));

            const savedChat = localStorage.getItem(STORAGE_KEYS.CHAT);
            if (savedChat) setChatMessages(JSON.parse(savedChat));

            const savedTemplates = localStorage.getItem(STORAGE_KEYS.TEMPLATES);
            if (savedTemplates) setMealTemplates(JSON.parse(savedTemplates));

            const savedMealPlans = localStorage.getItem(STORAGE_KEYS.MEAL_PLANS);
            if (savedMealPlans) setMealPlans(JSON.parse(savedMealPlans));
        } catch (error) {
            console.error("Failed to load data significantly:", error);
        } finally {
            setLoading(false);
        }
    }, []);

    // --- Persistence (Save) ---
    useEffect(() => {
        if (!loading) localStorage.setItem(STORAGE_KEYS.SETTINGS, JSON.stringify(settings));
    }, [settings, loading]);

    useEffect(() => {
        if (!loading) localStorage.setItem(STORAGE_KEYS.LOGS, JSON.stringify(logs));
    }, [logs, loading]);

    useEffect(() => {
        if (!loading) localStorage.setItem(STORAGE_KEYS.RECIPES, JSON.stringify(recipes));
    }, [recipes, loading]);

    useEffect(() => {
        if (!loading) localStorage.setItem(STORAGE_KEYS.WEIGHT_LOGS, JSON.stringify(weightLogs));
    }, [weightLogs, loading]);

    useEffect(() => {
        if (!loading) localStorage.setItem(STORAGE_KEYS.CHAT, JSON.stringify(chatMessages));
    }, [chatMessages, loading]);

    useEffect(() => {
        if (!loading) localStorage.setItem(STORAGE_KEYS.TEMPLATES, JSON.stringify(mealTemplates));
    }, [mealTemplates, loading]);

    useEffect(() => {
        if (!loading) localStorage.setItem(STORAGE_KEYS.MEAL_PLANS, JSON.stringify(mealPlans));
    }, [mealPlans, loading]);

    // --- Actions ---

    const updateSettings = (newSettings) => {
        setSettings((prev) => ({ ...prev, ...newSettings }));
    };

    const addLog = (entry) => {
        const newLog = {
            id: uuidv4(),
            timestamp: new Date().toISOString(),
            ...entry
        };
        setLogs((prev) => [newLog, ...prev]);
    };

    const removeLog = (id) => {
        setLogs((prev) => prev.filter(log => log.id !== id));
    };

    const addRecipe = (recipe) => {
        const newRecipe = {
            id: uuidv4(),
            createdAt: new Date().toISOString(),
            ...recipe
        };
        setRecipes((prev) => [...prev, newRecipe]);
    };

    const removeRecipe = (id) => {
        setRecipes((prev) => prev.filter(recipe => recipe.id !== id));
    };

    // Calculate totals for a specific date
    const getTotalsForDate = (dateStr) => {
        const targetDate = new Date(dateStr).toDateString();
        const dailyLogs = logs.filter(log => new Date(log.timestamp).toDateString() === targetDate);
        return dailyLogs.reduce((acc, log) => ({
            calories: acc.calories + (log.calories || 0),
            protein: acc.protein + (log.protein || 0),
            carbs: acc.carbs + (log.carbs || 0),
            fat: acc.fat + (log.fat || 0),
        }), { calories: 0, protein: 0, carbs: 0, fat: 0 });
    };

    const [currentDate, setCurrentDate] = useState(new Date().toISOString());

    const changeDate = (newDate) => {
        setCurrentDate(newDate);
    };

    const updateLog = (id, updatedEntry) => {
        setLogs((prev) => prev.map(log => log.id === id ? { ...log, ...updatedEntry } : log));
    };

    const updateRecipe = (id, updatedFields) => {
        setRecipes((prev) => prev.map(recipe => recipe.id === id ? { ...recipe, ...updatedFields } : recipe));
    };

    const logWeight = (weight, dateStr = new Date().toISOString()) => {
        const dateKey = new Date(dateStr).toDateString();
        setWeightLogs(prev => {
            const existing = prev.find(l => new Date(l.date).toDateString() === dateKey);
            if (existing) {
                return prev.map(l => l.id === existing.id ? { ...l, weight } : l);
            }
            return [...prev, { id: uuidv4(), date: dateStr, weight }];
        });
        updateSettings({ currentWeight: weight });
    };

    // --- Meal Templates ---

    const addTemplate = (template) => {
        const newTemplate = { id: uuidv4(), ...template };
        setMealTemplates(prev => [...prev, newTemplate]);
    };

    const removeTemplate = (id) => {
        setMealTemplates(prev => prev.filter(t => t.id !== id));
    };

    // --- Meal Plans ---

    const addMealPlanItem = (dateKey, mealType, item) => {
        setMealPlans(prev => {
            const dayPlan = prev[dateKey] || {};
            const mealItems = dayPlan[mealType] || [];
            return {
                ...prev,
                [dateKey]: {
                    ...dayPlan,
                    [mealType]: [...mealItems, { id: uuidv4(), ...item }],
                },
            };
        });
    };

    const removeMealPlanItem = (dateKey, mealType, itemId) => {
        setMealPlans(prev => {
            const dayPlan = prev[dateKey] || {};
            const mealItems = (dayPlan[mealType] || []).filter(i => i.id !== itemId);
            return {
                ...prev,
                [dateKey]: { ...dayPlan, [mealType]: mealItems },
            };
        });
    };

    const logPlannedMeal = (dateKey, mealType) => {
        const items = (mealPlans[dateKey] || {})[mealType] || [];
        const planDate = new Date(dateKey).toISOString();
        items.forEach(item => {
            addLog({
                food_name: item.food_name,
                calories: item.calories,
                protein: item.protein || 0,
                carbs: item.carbs || 0,
                fat: item.fat || 0,
                quantity: item.quantity || '1 serving',
                source: 'plan',
                meal_type: mealType,
                timestamp: planDate,
            });
        });
        // Clear the logged meal slot
        setMealPlans(prev => {
            const dayPlan = prev[dateKey] || {};
            return { ...prev, [dateKey]: { ...dayPlan, [mealType]: [] } };
        });
    };

    const value = {
        settings,
        logs,
        recipes,
        weightLogs,
        chatMessages,
        loading,
        currentDate,
        mealTemplates,
        mealPlans,
        updateSettings,
        setChatMessages,
        addLog,
        removeLog,
        updateLog,
        addRecipe,
        removeRecipe,
        updateRecipe,
        logWeight,
        getTotalsForDate,
        changeDate,
        addTemplate,
        removeTemplate,
        addMealPlanItem,
        removeMealPlanItem,
        logPlannedMeal,
    };

    return (
        <AppContext.Provider value={value}>
            {children}
        </AppContext.Provider>
    );
};
