export async function searchFoods(query) {
    if (!query.trim()) return [];
    try {
        const url = `https://world.openfoodfacts.org/cgi/search.pl?search_terms=${encodeURIComponent(query)}&json=1&page_size=10&search_simple=1&action=process`;
        const res = await fetch(url);
        if (!res.ok) return [];
        const data = await res.json();
        return (data.products || [])
            .filter(p => p.product_name && p.nutriments?.['energy-kcal_100g'])
            .map(p => ({
                food_name: p.product_name,
                brand: p.brands || '',
                calories: Math.round(p.nutriments['energy-kcal_100g'] || 0),
                protein: Math.round(p.nutriments['proteins_100g'] || 0),
                carbs: Math.round(p.nutriments['carbohydrates_100g'] || 0),
                fat: Math.round(p.nutriments['fat_100g'] || 0),
                serving_size: p.serving_size || '100g',
            }));
    } catch {
        return [];
    }
}
