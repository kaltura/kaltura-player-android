#!/bin/bash
zip main.zip *.py
aws lambda update-function-code --function-name getEntriesForPrefetch --zip-file fileb://main.zip
rm main.zip
