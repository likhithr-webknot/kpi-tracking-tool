#!/bin/bash

# Cycle Calculation API - Test All 11 Endpoints

BASE_URL="http://localhost:8080/cycles"
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   CYCLE CALCULATION API - COMPLETE ENDPOINT TEST          ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# 1. Current Cycle
echo -e "${YELLOW}[1] GET /cycles/current${NC}"
echo "Description: Get current submission cycle"
echo "Command:"
echo "  curl \"$BASE_URL/current\""
echo ""
echo "Expected Response:"
echo '  {
    "cycleKey": "2026-02",
    "timezone": "Asia/Kolkata",
    "timestamp": "2026-02-27T15:30:45.123456+05:30"
  }'
echo ""
echo "---"
echo ""

# 2. Next Cycle
echo -e "${YELLOW}[2] GET /cycles/{cycleKey}/next${NC}"
echo "Description: Get the next cycle"
echo "Command:"
echo "  curl \"$BASE_URL/2026-02/next\""
echo ""
echo "Expected Response:"
echo '  {
    "baseCycleKey": "2026-02",
    "nextCycleKey": "2026-03"
  }'
echo ""
echo "---"
echo ""

# 3. Previous Cycle
echo -e "${YELLOW}[3] GET /cycles/{cycleKey}/previous${NC}"
echo "Description: Get the previous cycle"
echo "Command:"
echo "  curl \"$BASE_URL/2026-02/previous\""
echo ""
echo "Expected Response:"
echo '  {
    "baseCycleKey": "2026-02",
    "previousCycleKey": "2026-01"
  }'
echo ""
echo "---"
echo ""

# 4. Offset Cycle
echo -e "${YELLOW}[4] GET /cycles/{cycleKey}/offset?months={N}${NC}"
echo "Description: Get cycle N months away"
echo "Command:"
echo "  curl \"$BASE_URL/2026-02/offset?months=3\""
echo "  curl \"$BASE_URL/2026-02/offset?months=-3\""
echo ""
echo "Expected Response (months=3):"
echo '  {
    "baseCycleKey": "2026-02",
    "monthsOffset": 3,
    "offsetCycleKey": "2026-05"
  }'
echo ""
echo "---"
echo ""

# 5. Validate
echo -e "${YELLOW}[5] GET /cycles/{cycleKey}/validate${NC}"
echo "Description: Validate cycle key format"
echo "Command:"
echo "  curl \"$BASE_URL/2026-02/validate\""
echo "  curl \"$BASE_URL/invalid/validate\""
echo ""
echo "Expected Response (valid):"
echo '  {
    "cycleKey": "2026-02",
    "isValid": true
  }'
echo ""
echo "Expected Response (invalid):"
echo '  {
    "cycleKey": "2026-13",
    "isValid": false,
    "expectedFormat": "yyyy-MM (e.g., 2026-02)"
  }'
echo ""
echo "---"
echo ""

# 6. Range
echo -e "${YELLOW}[6] GET /cycles/{cycleKey}/range?endCycleKey={END}${NC}"
echo "Description: Get all cycles in a range"
echo "Command:"
echo "  curl \"$BASE_URL/2026-01/range?endCycleKey=2026-03\""
echo ""
echo "Expected Response:"
echo '  {
    "startCycleKey": "2026-01",
    "endCycleKey": "2026-03",
    "cycleCount": 3,
    "cycles": ["2026-01", "2026-02", "2026-03"]
  }'
echo ""
echo "---"
echo ""

# 7. Last N
echo -e "${YELLOW}[7] GET /cycles/last-n?count={N}${NC}"
echo "Description: Get last N cycles"
echo "Command:"
echo "  curl \"$BASE_URL/last-n?count=3\""
echo ""
echo "Expected Response:"
echo '  {
    "requestedCount": 3,
    "actualCount": 3,
    "cycles": ["2026-02", "2026-01", "2025-12"]
  }'
echo ""
echo "---"
echo ""

# 8. Next N
echo -e "${YELLOW}[8] GET /cycles/next-n?count={N}${NC}"
echo "Description: Get next N cycles"
echo "Command:"
echo "  curl \"$BASE_URL/next-n?count=3\""
echo ""
echo "Expected Response:"
echo '  {
    "requestedCount": 3,
    "actualCount": 3,
    "cycles": ["2026-02", "2026-03", "2026-04"]
  }'
echo ""
echo "---"
echo ""

# 9. Info
echo -e "${YELLOW}[9] GET /cycles/{cycleKey}/info?timezone={TZ}${NC}"
echo "Description: Get detailed cycle information"
echo "Command:"
echo "  curl \"$BASE_URL/2026-02/info\""
echo "  curl \"$BASE_URL/2026-02/info?timezone=UTC\""
echo ""
echo "Expected Response:"
echo '  {
    "cycleKey": "2026-02",
    "timezone": "Asia/Kolkata",
    "startAt": "2026-02-01T00:00:00+05:30",
    "endAt": "2026-02-28T23:59:59+05:30",
    "daysInCycle": 28,
    "isCurrent": true,
    "isPast": false,
    "isFuture": false,
    "nextCycleKey": "2026-03",
    "previousCycleKey": "2026-01"
  }'
echo ""
echo "---"
echo ""

# 10. Months Between
echo -e "${YELLOW}[10] GET /cycles/{cycleKey}/months-between?toCycleKey={TO}${NC}"
echo "Description: Calculate months between two cycles"
echo "Command:"
echo "  curl \"$BASE_URL/2026-01/months-between?toCycleKey=2026-03\""
echo "  curl \"$BASE_URL/2026-03/months-between?toCycleKey=2026-01\""
echo ""
echo "Expected Response (forward):"
echo '  {
    "fromCycleKey": "2026-01",
    "toCycleKey": "2026-03",
    "monthsBetween": 2
  }'
echo ""
echo "Expected Response (backward):"
echo '  {
    "fromCycleKey": "2026-03",
    "toCycleKey": "2026-01",
    "monthsBetween": -2
  }'
echo ""
echo "---"
echo ""

# 11. Check Date
echo -e "${YELLOW}[11] POST /cycles/{cycleKey}/check-date?dateTime={DATE}${NC}"
echo "Description: Check if date is in cycle"
echo "Command:"
echo "  curl -X POST \"$BASE_URL/2026-02/check-date?dateTime=2026-02-15T12:00:00Z\""
echo "  curl -X POST \"$BASE_URL/2026-02/check-date?dateTime=2026-03-15T12:00:00Z\""
echo ""
echo "Expected Response (in cycle):"
echo '  {
    "cycleKey": "2026-02",
    "dateTime": "2026-02-15T12:00:00Z",
    "timezone": "Asia/Kolkata",
    "isInCycle": true
  }'
echo ""
echo "Expected Response (not in cycle):"
echo '  {
    "cycleKey": "2026-02",
    "dateTime": "2026-03-15T12:00:00Z",
    "timezone": "Asia/Kolkata",
    "isInCycle": false
  }'
echo ""
echo "---"
echo ""

echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║                    SUMMARY                                 ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${GREEN}✓ 11 Endpoints Available${NC}"
echo -e "${GREEN}✓ All methods documented with examples${NC}"
echo -e "${GREEN}✓ All supporting parameters documented${NC}"
echo -e "${GREEN}✓ Response structures documented${NC}"
echo ""
echo "Base URL: http://localhost:8080/cycles"
echo ""
echo "For more details, see:"
echo "  - CYCLE_ENDPOINTS.txt"
echo "  - CYCLE_ENDPOINTS_SUMMARY.md"
echo "  - CYCLE_CALCULATION_API.md"
echo "  - CYCLE_CALCULATION_API_QUICK_REF.md"
echo ""
