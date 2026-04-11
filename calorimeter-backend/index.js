require('dotenv').config();

const admin = require("firebase-admin");

const serviceAccount = JSON.parse(process.env.FIREBASE_KEY);

// ✅ Prevent duplicate initialization
if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
  });
}
const express = require('express');
const app = express();
const axios = require('axios');

app.use(express.json());


app.use((req, res, next) => {
  const key = req.headers['x-api-key'];

  console.log("Incoming:", key);
  console.log("Expected:", process.env.SECRET_API_KEY);

  if (key !== process.env.SECRET_API_KEY) {
    return res.status(403).send("Unauthorized");
  }

  next();
});

admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
const db = admin.firestore();
const COLLECTION = 'foods';

// ✅ ADD FOOD
app.post('/add-food', async (req, res) => {
  try {
    const { uid, foodName, calories, protein, carbs, fat, imageUrl, quantity, unit } = req.body;
    const foodData = {
      uid, foodName,
      calories: Math.round(Number(calories)) || 0,
      protein: Number(protein) || 0,
      carbs: Number(carbs) || 0,
      fat: Number(fat) || 0,
      imageUrl: imageUrl || "",
      quantity: Number(quantity) || 1,
      unit: unit || "serving",
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    };
    const docRef = await db.collection(COLLECTION).add(foodData);
    res.send({ message: "Success", id: docRef.id });
  } catch (error) { res.status(500).send(error.message); }
});

// ✅ GET FOODS
app.get('/get-foods/:uid', async (req, res) => {
  try {
    const snapshot = await db.collection(COLLECTION).where('uid', '==', req.params.uid).get();
    let foods = [];
    snapshot.forEach(doc => {
      const d = doc.data();
      foods.push({ id: doc.id, ...d, _ts: d.createdAt ? d.createdAt.toMillis() : Date.now() });
    });
    foods.sort((a, b) => b._ts - a._ts);
    res.send(foods);
  } catch (error) { res.status(500).send(error.message); }
});

// 🗑️ DELETE FOOD
app.delete('/delete-food/:id', async (req, res) => {
  try {
    await db.collection(COLLECTION).doc(req.params.id).delete();
    res.send({ message: "Deleted" });
  } catch (error) { res.status(500).send(error.message); }
});

// 🔍 SEARCH FOOD
app.get('/search-food', async (req, res) => {
  try {
    const { name } = req.query;
    const usdaRes = await axios.get(`https://api.nal.usda.gov/fdc/v1/foods/search?query=${encodeURIComponent(name)}&api_key=${process.env.USDA_API_KEY}`);
    const food = usdaRes.data.foods[0];
    if (!food) return res.status(404).send("Not found");

    let nutrients = { calories: 0, protein: 0, carbs: 0, fat: 0 };
    food.foodNutrients.forEach(n => {
      const nid = Number(n.nutrientId);
      const nname = n.nutrientName.toLowerCase();
      if ([1008, 2047, 2048].includes(nid) || nname.includes("energy")) if (n.unitName === "KCAL") nutrients.calories = n.value;
      if (nid === 1003 || nname === "protein") nutrients.protein = n.value;
      if (nid === 1005 || nname.includes("carbohydrate")) nutrients.carbs = n.value;
      if (nid === 1004 || nname.includes("lipid") || nname === "fat") nutrients.fat = n.value;
    });

    let imageUrl = "";
    try {
      const cleanQ = name.split(',')[0].split(' ')[0];
      const pixRes = await axios.get(`https://pixabay.com/api/?key=${process.env.PIXABAY_API_KEY}&q=${encodeURIComponent(cleanQ + " food")}&image_type=photo&category=food&safesearch=true`);
      if (pixRes.data.hits.length > 0) imageUrl = pixRes.data.hits[0].webformatURL;
    } catch (e) {}

    res.send({ name: food.description, ...nutrients, imageUrl });
  } catch (err) { res.status(500).send(err.message); }
});

// 📷 BARCODE SEARCH
app.get('/barcode/:code', async (req, res) => {
  try {
    const r = await axios.get(`https://world.openfoodfacts.org/api/v0/product/${req.params.code}.json`);
    if (r.data.status === 0) return res.status(404).send("Not found");
    const p = r.data.product;
    res.send({
      name: p.product_name || "Unknown",
      calories: p.nutriments['energy-kcal_100g'] || 0,
      protein: p.nutriments.proteins_100g || 0,
      carbs: p.nutriments.carbohydrates_100g || 0,
      fat: p.nutriments.fat_100g || 0,
      imageUrl: p.image_url || ""
    });
  } catch (err) { res.status(500).send(err.message); }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Server running on ${PORT}`));