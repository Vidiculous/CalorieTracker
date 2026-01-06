
import React, { useState } from 'react';
import { useZxing } from 'react-zxing';
import { X, CheckCircle } from 'lucide-react';
import { useAppContext } from '../context/AppContext';

const BarcodeScanner = ({ onClose, onScanComplete }) => {
    const { addLog } = useAppContext();
    const [scannedCode, setScannedCode] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const { ref } = useZxing({
        onDecodeResult: (result) => {
            if (!scannedCode && !loading) {
                handleScan(result.getText());
            }
        },
        onError: (err) => {
            console.warn("Scanner error:", err);
            // Only set error if it's a critical camera access issue, not transient read errors
            if (err.name === 'NotAllowedError' || err.name === 'NotFoundError') {
                setError("Camera not accessible");
            }
        },
        constraints: {
            video: {
                facingMode: { ideal: "environment" }
            }
        }
    });

    const handleScan = async (code) => {
        setScannedCode(code);
        setLoading(true);
        try {
            // Using OpenFoodFacts API (free, public)
            const response = await fetch(`https://world.openfoodfacts.org/api/v0/product/${code}.json`);
            const data = await response.json();

            if (data.status === 1) {
                const product = data.product;
                const name = product.product_name || "Unknown Product";
                // OFF returns kcal per 100g usually. Let's assume 100g serving for simplicity or 1 serving size if available.
                // It's tricky without user confirming amount.
                // We'll try to find 'energy-kcal_100g' or 'energy-kcal_serving'

                const calories = product.nutriments['energy-kcal_serving'] || product.nutriments['energy-kcal_100g'] || 0;
                const protein = product.nutriments['proteins_serving'] || product.nutriments['proteins_100g'] || 0;
                const carbs = product.nutriments['carbohydrates_serving'] || product.nutriments['carbohydrates_100g'] || 0;
                const fat = product.nutriments['fat_serving'] || product.nutriments['fat_100g'] || 0;

                const logEntry = {
                    food_name: name,
                    calories: Math.round(calories),
                    protein: Math.round(protein),
                    carbs: Math.round(carbs),
                    fat: Math.round(fat),
                    quantity: "1 serving (est)",
                    source: 'barcode'
                };

                addLog(logEntry);
                onScanComplete(logEntry);
            } else {
                setError("Product not found");
                setTimeout(() => {
                    setScannedCode(null);
                    setLoading(false);
                    setError(null);
                }, 2000);
            }
        } catch (err) {
            console.error(err);
            setError("Failed to fetch product data");
            setTimeout(() => {
                setScannedCode(null);
                setLoading(false);
                setError(null);
            }, 2000);
        } finally {
            // If successful, onScanComplete handles close. 
            // If error, we reset in timeout.
        }
    };

    return (
        <div className="fixed inset-0 z-[60] bg-black bg-opacity-90 flex flex-col items-center justify-center">
            <button onClick={onClose} className="absolute top-4 right-4 text-white p-2 bg-neutral-800 rounded-full">
                <X size={24} />
            </button>
            <div className="w-full max-w-sm aspect-[3/4] bg-neutral-900 rounded-3xl overflow-hidden relative border border-neutral-700">
                {!scannedCode ? (
                    <>
                        <video ref={ref} className="w-full h-full object-cover" />
                        <div className="absolute inset-0 border-2 border-rose-500/50 m-12 rounded-2xl pointer-events-none animate-pulse"></div>
                        <p className="absolute bottom-8 left-0 right-0 text-center text-white/80 font-bold">Scanning...</p>
                    </>
                ) : (
                    <div className="flex flex-col items-center justify-center h-full space-y-4">
                        {loading ? (
                            <div className="animate-spin h-8 w-8 border-4 border-rose-500 border-t-transparent rounded-full"></div>
                        ) : error ? (
                            <p className="text-red-500 font-bold">{error}</p>
                        ) : (
                            <CheckCircle size={48} className="text-green-500" />
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default BarcodeScanner;
