#!/bin/bash

# Start Full PAN12 Training
echo "🚀 Starting Full PAN12 Training..."
echo "=================================="
echo ""

cd ~/AndroidStudioProjects/KidGuard

# Check if data exists
if [ ! -f "training/data/pan12_full/pan12_train_full.json" ]; then
    echo "❌ Training data not found!"
    echo "   Run: python3 training/parse_pan12_full.py"
    exit 1
fi

echo "✅ Training data found"
echo ""

# Start training in background
python3 training/train_pan12_full.py > training/pan12_full_training.log 2>&1 &
TRAIN_PID=$!

echo "✅ Training started (PID: $TRAIN_PID)"
echo ""
echo "📊 Monitor progress:"
echo "   tail -f training/pan12_full_training.log"
echo ""
echo "🛑 Stop training:"
echo "   kill $TRAIN_PID"
echo ""
echo "⏰ Expected duration: 2-3 hours"
echo "🎯 Expected accuracy: 96-98%"
echo ""
echo "🔄 Watching first 2 minutes..."
echo "=================================="

# Watch for 2 minutes
for i in {1..24}; do
    sleep 5
    if [ -f "training/pan12_full_training.log" ]; then
        echo ""
        echo "--- Progress at $(date +%H:%M:%S) ---"
        tail -10 training/pan12_full_training.log
    fi
done

echo ""
echo "✅ Training is running in background!"
echo "   Continue monitoring: tail -f training/pan12_full_training.log"
