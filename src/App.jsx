import React, { useState, useEffect, useRef } from 'react';
import { v4 as uuidv4 } from 'uuid';
import { AppProvider, useAppContext } from './context/AppContext';
import Layout from './layouts/Layout';
import Onboarding from './components/Onboarding';
import Dashboard from './components/Dashboard';
import ChatInterface from './components/ChatInterface';
import Analytics from './components/Analytics';
import BarcodeScanner from './components/BarcodeScanner';
import EditLogModal from './components/EditLogModal';
import ManualEntryModal from './components/ManualEntryModal';
import { analyzeText, analyzeImage, transcribeAudio, listAvailableModels } from './services/ai';
import RecipeList from './components/RecipeList';
import { Camera, BookOpen, ScanBarcode, Mic } from 'lucide-react';


const AppContent = () => {
  const {
    settings,
    loading,
    addLog,
    updateSettings,
    logs,
    removeLog,
    updateLog,
    addRecipe,
    currentDate,
    getTotalsForDate
  } = useAppContext();

  // Debug: Check available models on load
  React.useEffect(() => {
    if (settings.apiKey) {
      listAvailableModels(settings.apiKey);
    }
  }, [settings.apiKey, listAvailableModels]);
  const [showSettings, setShowSettings] = useState(false);
  const [showChat, setShowChat] = useState(false);
  const [showRecipes, setShowRecipes] = useState(false);
  const [chatInitialMode, setChatInitialMode] = useState(null);
  const [showAnalytics, setShowAnalytics] = useState(false);
  const [showBarcode, setShowBarcode] = useState(false);
  const [showManual, setShowManual] = useState(false);
  const [editingLog, setEditingLog] = useState(null);

  const handleRecipeLog = (recipe) => {
    addLog({
      ...recipe,
      food_name: recipe.name,
      source: 'recipe',
      timestamp: currentDate
    });
    setShowRecipes(false);
  };

  // ... (omitting lines to keep replacement concise, focusing on relevant blocks)

  const openChatWithMode = (mode) => {
    setChatInitialMode(mode);
    setShowChat(true);
  };

  // ...


  // ... (handleSend remains same) ...

  const handleSend = async (data, type, history = []) => {
    if (!settings.apiKey) {
      return { message: "Please set your Gemini API Key in Settings first!" };
    }

    // Prepare Context (Current Calories vs Limit)
    const todaysTotals = getTotalsForDate(currentDate);

    // settings usually has dailyGoal, but some legacy might use dailyCalorieTarget. Check both.
    const calorieLimit = settings.dailyGoal || settings.dailyCalorieTarget || 2000;

    const aiContext = {
      current: Math.round(todaysTotals.calories),
      limit: calorieLimit,
      macros: {
        protein: { current: Math.round(todaysTotals.protein), target: settings.proteinGoal || 150 },
        carbs: { current: Math.round(todaysTotals.carbs), target: settings.carbsGoal || 250 },
        fat: { current: Math.round(todaysTotals.fat), target: settings.fatGoal || 70 }
      },
      currentLogs: todaysTotals.logs?.map(l => ({ id: l.id, name: l.food_name, calories: l.calories })),
      history: history // Pass history in context object
    };



    // ...

    let result;
    if (type === 'text') {
      result = await analyzeText(settings.apiKey, settings.selectedModel, data, aiContext);
    } else if (type === 'image') {
      result = await analyzeImage(settings.apiKey, settings.selectedModel, data, aiContext);
    } else if (type === 'transcribe') {
      const transcription = await transcribeAudio(settings.apiKey, settings.selectedModel, data.audio);
      return { transcription: transcription.text };
    } else if (type === 'recipe') {

      // Direct logging from RecipeList (UI initiated)
      addLog({
        ...data,
        food_name: data.name,
        source: 'recipe',
        timestamp: currentDate
      });
      return { message: `Logged ${data.name} from recipes.` };
    }

    if (result && !result.error) {
      const baseResponse = { transcription: result.transcription };
      const explanationText = result.explanation ? `📝 ${result.explanation}\n\n` : '';

      // 0. Handle Conversation / QA
      if (result.type === 'conversation') {
        return { ...baseResponse, message: `${explanationText}${result.answer || "I'm not sure, but here makes sense."}` };
      }

      // 1. Handle Clarifications
      if (result.type === 'clarification') {
        return { ...baseResponse, message: `${explanationText}${result.question || "Could you provide more details?"}` };
      }

      // 1.5. Handle Updates to existing logs
      if (result.type === 'update' && result.update_target_id) {
        const items = result.items ? result.items : [result];
        const item = items[0]; // Usually update one item

        updateLog(result.update_target_id, {
          food_name: item.food_name,
          calories: item.calories,
          protein: item.protein,
          carbs: item.carbs,
          fat: item.fat,
          quantity: item.quantity_desc
        });

        return { ...baseResponse, message: `${explanationText}${result.status_message || "Updated your log with the new details."}` };
      }

      // 2. Handle New Recipes
      if (result.type === 'recipe') {
        // Calculate total nutrition for the recipe or per serving if provided
        const items = result.items || [];
        const totalCalories = items.reduce((sum, item) => sum + (item.calories || 0), 0);
        const totalProtein = items.reduce((sum, item) => sum + (item.protein || 0), 0);
        const totalCarbs = items.reduce((sum, item) => sum + (item.carbs || 0), 0);
        const totalFat = items.reduce((sum, item) => sum + (item.fat || 0), 0);

        const recipeName = result.meal_name || items.map(i => i.food_name).join(', ');

        const servings = result.recipe_details?.servings || 1;
        const caloriesPerServing = Math.round(totalCalories / servings);

        addRecipe({
          name: recipeName,
          calories: totalCalories,
          protein: totalProtein,
          carbs: totalCarbs,
          fat: totalFat,
          items: items,
          servings: servings
        });
        return { ...baseResponse, message: `${explanationText}${result.status_message || `Saved recipe: ${recipeName} (${caloriesPerServing} kcal/serving).`}` };
      }


      // 3. Defaults to 'log' (Immediate logging)
      // Handle new multi-item schema
      const items = result.items ? result.items : [result];

      if (items.length > 1) {
        // Group into single meal log
        const totalCalories = items.reduce((sum, item) => sum + (item.calories || 0), 0);
        const totalProtein = items.reduce((sum, item) => sum + (item.protein || 0), 0);
        const totalCarbs = items.reduce((sum, item) => sum + (item.carbs || 0), 0);
        const totalFat = items.reduce((sum, item) => sum + (item.fat || 0), 0);
        const mealName = result.meal_name || items.map(i => i.food_name).join(' & ').substring(0, 30) + '...';

        addLog({
          food_name: mealName,
          calories: totalCalories,
          protein: totalProtein,
          carbs: totalCarbs,
          fat: totalFat,
          quantity: `${items.length} items`,
          source: type,
          timestamp: currentDate,
          items: items.map(i => ({ ...i, id: uuidv4() })), // Store sub-items with IDs,
          meal_type: result.meal_type // AI suggested meal type
        });
        return { ...baseResponse, message: `${explanationText}${result.status_message || `Logged meal: ${mealName} (${totalCalories} kcal)`}` };

      } else if (items.length === 1) {
        // Single item - log normally
        const item = items[0];
        if (item.calories) {
          addLog({
            food_name: item.food_name || "Unknown Food",
            calories: item.calories,
            protein: item.protein || 0,
            carbs: item.carbs || 0,
            fat: item.fat || 0,
            quantity: item.quantity_desc || "",
            source: type,
            timestamp: currentDate,
            meal_type: result.meal_type // AI suggested meal type
          });
          return { ...baseResponse, message: `${explanationText}${result.status_message || `Logged: ${item.food_name} (${item.calories} kcal)`}` };
        }
      }

      return { ...baseResponse, message: `${explanationText}I could not identify any food items with calories. Please try again.` };
    } else {
      return { message: result?.error || "AI Error occurred." };
    }
  };

  if (loading) {
    return <div className="min-h-screen bg-neutral-900 flex items-center justify-center text-white">Loading...</div>;
  }

  if (!settings.hasOnboarded) {
    return <Onboarding />;
  }



  // ... (existing code) ...

  return (
    <Layout onOpenSettings={() => setShowSettings(!showSettings)}>

      <div className="flex flex-col min-h-screen pb-24"> {/* pb-24 for FAB space */}
        <div className="flex-shrink-0">
          <Dashboard
            onOpenAnalytics={() => setShowAnalytics(true)}
            onEditLog={(log) => setEditingLog(log)}
            onOpenManual={() => setShowManual(true)}
          />
        </div>
      </div>

      {/* Persistent Chat Input Trigger */}
      <div className="fixed bottom-0 left-0 right-0 p-4 z-50 bg-gradient-to-t from-black/80 to-transparent">
        <div className="max-w-md mx-auto bg-neutral-800/90 backdrop-blur-md border border-neutral-700 p-2 rounded-3xl flex items-center gap-2 shadow-2xl cursor-pointer" onClick={() => openChatWithMode(null)}>
          <div className="flex gap-1">
            <button
              onClick={(e) => { e.stopPropagation(); openChatWithMode('camera'); }}
              className="p-3 text-neutral-400 hover:text-white hover:bg-neutral-700/50 rounded-xl transition-colors"
            >
              <Camera size={20} />
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); setShowRecipes(true); }}
              className="p-3 text-neutral-400 hover:text-white hover:bg-neutral-700/50 rounded-xl transition-colors"
            >
              <BookOpen size={20} />
            </button>
            <button
              onClick={(e) => { e.stopPropagation(); setShowBarcode(true); }}
              className="p-3 text-neutral-400 hover:text-white hover:bg-neutral-700/50 rounded-xl transition-colors"
            >
              <ScanBarcode size={20} />
            </button>
          </div>

          <div className="flex-1 bg-neutral-900/50 rounded-2xl p-3 flex items-center justify-between border border-neutral-700/50 group hover:border-neutral-600 transition-colors cursor-text" onClick={() => openChatWithMode(null)}>
            <span className="text-neutral-500 text-sm group-hover:text-neutral-400">Type, paste URL, or speak...</span>
            <button
              onClick={(e) => { e.stopPropagation(); openChatWithMode('voice'); }}
              className="p-1.5 -mr-1.5 rounded-lg hover:bg-neutral-800 text-neutral-500 hover:text-white transition-colors"
            >
              <Mic size={18} />
            </button>
          </div>
        </div>
      </div>

      {showRecipes && (
        <RecipeList
          onClose={() => setShowRecipes(false)}
          onSelect={handleRecipeLog}
        />
      )}

      {/* Chat Modal - Full Screen Overlay */}
      {showChat && (
        <div className="fixed inset-0 z-[60] bg-neutral-900/95 backdrop-blur-sm flex items-center justify-center p-0 md:p-6 animate-in slide-in-from-bottom-10 duration-300">
          <div className="w-full h-full md:max-w-md md:h-[80vh] bg-neutral-900 md:bg-transparent relative">
            <ChatInterface
              onSend={handleSend}
              onOpenBarcode={() => setShowBarcode(true)}
              onClose={() => { setShowChat(false); setChatInitialMode(null); }}
              initialMode={chatInitialMode}
            />
          </div>
        </div>
      )}


      {/* Simple Settings Modal Placeholder */}
      {
        showSettings && (
          <div className="fixed inset-0 z-[60] bg-black/50 backdrop-blur-sm flex items-center justify-center p-4" onClick={() => setShowSettings(false)}>
            <div className="bg-neutral-900 border border-neutral-800 p-6 rounded-3xl w-full max-w-xs" onClick={e => e.stopPropagation()}>
              <h2 className="text-xl font-bold text-white mb-4">Settings</h2>
              <div className="mb-4 space-y-3">
                <div>
                  <label className="text-xs text-neutral-500 font-bold block mb-1">CALORIE GOAL</label>
                  <input
                    type="number"
                    value={settings.dailyGoal}
                    onChange={(e) => updateSettings({ dailyGoal: Number(e.target.value) })}
                    className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-2 text-white text-sm"
                  />
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="text-xs text-neutral-500 font-bold block mb-1">CURRENT WEIGHT (KG)</label>
                    <input
                      type="number"
                      value={settings.currentWeight || ''}
                      onChange={(e) => updateSettings({ currentWeight: Number(e.target.value) })}
                      className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-2 text-white text-sm"
                    />
                  </div>
                  <div>
                    <label className="text-xs text-neutral-500 font-bold block mb-1">GOAL WEIGHT (KG)</label>
                    <input
                      type="number"
                      value={settings.goalWeight || ''}
                      onChange={(e) => updateSettings({ goalWeight: Number(e.target.value) })}
                      className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-2 text-white text-sm"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-3 gap-2">
                  <div>
                    <label className="text-xs text-neutral-500 font-bold block mb-1">PROTEIN</label>
                    <input
                      type="number"
                      value={settings.proteinGoal || 150}
                      onChange={(e) => updateSettings({ proteinGoal: Number(e.target.value) })}
                      className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-2 text-white text-sm"
                    />
                  </div>
                  <div>
                    <label className="text-xs text-neutral-500 font-bold block mb-1">CARBS</label>
                    <input
                      type="number"
                      value={settings.carbsGoal || 250}
                      onChange={(e) => updateSettings({ carbsGoal: Number(e.target.value) })}
                      className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-2 text-white text-sm"
                    />
                  </div>
                  <div>
                    <label className="text-xs text-neutral-500 font-bold block mb-1">FAT</label>
                    <input
                      type="number"
                      value={settings.fatGoal || 70}
                      onChange={(e) => updateSettings({ fatGoal: Number(e.target.value) })}
                      className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-2 text-white text-sm"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-xs text-neutral-500 font-bold block mb-1">GEMINI API KEY</label>
                  <input
                    type="password"
                    value={settings.apiKey || ''}
                    onChange={(e) => updateSettings({ apiKey: e.target.value })}
                    placeholder="Paste Key Here"
                    className="w-full bg-neutral-800 border border-neutral-700 rounded-xl p-2 text-white text-sm"
                  />
                </div>
              </div>
              <button
                onClick={() => setShowSettings(false)}
                className="w-full bg-neutral-800 hover:bg-neutral-700 text-white p-3 rounded-xl font-bold"
              >
                Close
              </button>
            </div>
          </div>
        )
      }

      {showAnalytics && <Analytics onClose={() => setShowAnalytics(false)} />}

      {
        showBarcode && (
          <BarcodeScanner
            onClose={() => setShowBarcode(false)}
            onScanComplete={() => setShowBarcode(false)}
          />
        )
      }

      {
        showManual && (
          <ManualEntryModal
            onClose={() => setShowManual(false)}
            onAdd={(data) => {
              addLog({ ...data, timestamp: currentDate });
            }}
          />
        )
      }

      {
        editingLog && (
          <EditLogModal
            entry={editingLog}
            onClose={() => setEditingLog(null)}
            onDelete={(id) => {
              if (editingLog.parentId) {
                // Deleting a sub-item
                const parent = logs.find(l => l.id === editingLog.parentId);
                if (parent) {
                  const newItems = parent.items.filter(i => i.id !== id);
                  if (newItems.length === 0) {
                    removeLog(parent.id);
                  } else {
                    // Recalc totals
                    const totalCalories = newItems.reduce((sum, item) => sum + (item.calories || 0), 0);
                    const totalProtein = newItems.reduce((sum, item) => sum + (item.protein || 0), 0);
                    const totalCarbs = newItems.reduce((sum, item) => sum + (item.carbs || 0), 0);
                    const totalFat = newItems.reduce((sum, item) => sum + (item.fat || 0), 0);

                    updateLog(parent.id, {
                      ...parent,
                      calories: totalCalories,
                      protein: totalProtein,
                      carbs: totalCarbs,
                      fat: totalFat,
                      items: newItems,
                      quantity: `${newItems.length} items`
                    });
                  }
                }
              } else {
                removeLog(id);
              }
              setEditingLog(null);
            }}
            onSave={(id, newData) => {
              if (editingLog.parentId) {
                // Editing a sub-item
                const parent = logs.find(l => l.id === editingLog.parentId);
                if (parent) {
                  const newItems = parent.items.map(i => i.id === id ? { ...i, ...newData } : i);

                  // Recalc totals
                  const totalCalories = newItems.reduce((sum, item) => sum + (item.calories || 0), 0);
                  const totalProtein = newItems.reduce((sum, item) => sum + (item.protein || 0), 0);
                  const totalCarbs = newItems.reduce((sum, item) => sum + (item.carbs || 0), 0);
                  const totalFat = newItems.reduce((sum, item) => sum + (item.fat || 0), 0);

                  updateLog(parent.id, {
                    ...parent,
                    calories: totalCalories,
                    protein: totalProtein,
                    carbs: totalCarbs,
                    fat: totalFat,
                    items: newItems
                  });
                }
              } else {
                updateLog(id, {
                  ...newData,
                  source: editingLog.source || 'manual',
                  timestamp: editingLog.timestamp
                });
              }
              setEditingLog(null);
            }}
          />
        )
      }
    </Layout >
  );
};

function App() {
  return (
    <AppProvider>
      <AppContent />
    </AppProvider>
  )
}

export default App
