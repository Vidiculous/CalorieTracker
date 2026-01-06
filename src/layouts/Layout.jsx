
import React from 'react';
import { Settings } from 'lucide-react';

const Layout = ({ children, onOpenSettings }) => {
    return (
        <div className="min-h-screen bg-neutral-900 text-white font-sans selection:bg-rose-500 selection:text-white">
            <header className="fixed top-0 left-0 right-0 z-50 bg-neutral-900/80 backdrop-blur-md border-b border-neutral-800 p-4">
                <div className="max-w-md mx-auto flex justify-between items-center">
                    <h1 className="text-xl font-bold bg-gradient-to-r from-rose-500 to-orange-500 bg-clip-text text-transparent">
                        CalorieTracker
                    </h1>
                    <button
                        onClick={onOpenSettings}
                        className="p-2 hover:bg-neutral-800 rounded-full transition-colors text-neutral-400 hover:text-white"
                    >
                        <Settings size={20} />
                    </button>
                </div>
            </header>

            <main className="pt-20 pb-24 px-4 max-w-md mx-auto min-h-screen">
                {children}
            </main>
        </div>
    );
};

export default Layout;
