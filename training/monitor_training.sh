#!/bin/bash

# KidGuard Training Monitor
# Usage: ./monitor_training.sh

echo "🔍 KidGuard Training Monitor"
echo "=============================="
echo ""

# Check if training is running
TRAINING_PID=$(ps aux | grep "train_quick.py" | grep -v grep | awk '{print $2}')

if [ -z "$TRAINING_PID" ]; then
    echo "❌ No training process found"
    echo ""

    # Check if training completed
    if [ -f "training/models/quick_model.keras" ]; then
        echo "✅ Training appears to be COMPLETE!"
        echo ""

        # Show final results
        echo "📊 Final Results:"
        echo "─────────────────"
        grep -A 2 "Test Accuracy" training/quick_training.log 2>/dev/null || echo "   (Log not found)"

        echo ""
        echo "📁 Model saved:"
        ls -lh training/models/quick_model.keras 2>/dev/null

        echo ""
        echo "💡 Next steps:"
        echo "   1. Review results: cat training/quick_training.log"
        echo "   2. Start comprehensive training: python3 training/train_comprehensive.py"

    else
        echo "⚠️  Training not running and no model found"
        echo ""
        echo "💡 Start training with:"
        echo "   python3 training/train_quick.py > training/quick_training.log 2>&1 &"
    fi

    exit 0
fi

echo "✅ Training is RUNNING (PID: $TRAINING_PID)"
echo ""

# Show resource usage
echo "💻 Resource Usage:"
echo "─────────────────"
ps -p $TRAINING_PID -o %cpu,%mem,etime,command 2>/dev/null || echo "   (Process info unavailable)"
echo ""

# Show last 25 lines of log
echo "📋 Recent Training Output:"
echo "─────────────────────────"
if [ -f "training/quick_training.log" ]; then
    tail -25 training/quick_training.log

    echo ""
    echo "─────────────────────────"

    # Try to extract current epoch
    CURRENT_EPOCH=$(grep -o "Epoch [0-9]*/[0-9]*" training/quick_training.log | tail -1)
    if [ ! -z "$CURRENT_EPOCH" ]; then
        echo "📊 Progress: $CURRENT_EPOCH"
    fi

    # Try to extract current accuracy
    CURRENT_ACC=$(grep "val_accuracy" training/quick_training.log | tail -1 | grep -o "val_accuracy: [0-9.]*" | grep -o "[0-9.]*$")
    if [ ! -z "$CURRENT_ACC" ]; then
        ACC_PERCENT=$(echo "$CURRENT_ACC * 100" | bc)
        echo "🎯 Current Val Accuracy: ${ACC_PERCENT}%"
    fi

else
    echo "   (Log file not found)"
fi

echo ""
echo "💡 Commands:"
echo "   Watch live:  tail -f training/quick_training.log"
echo "   Stop:        kill $TRAINING_PID"
echo "   Re-run:      ./monitor_training.sh"
