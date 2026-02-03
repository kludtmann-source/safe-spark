#!/bin/bash
# Training Wrapper Script

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
echo "Starting training at $(date)"
echo "=================================="

python3 -u training/train_pan12_fixed.py 2>&1

echo "=================================="
echo "Training finished at $(date)"
