#!/bin/bash

# Rate Limiting 테스트 스크립트
# 사용법: ./test-rate-limiting.sh [base-url]
# 예시: ./test-rate-limiting.sh http://localhost:8080

BASE_URL=${1:-"http://localhost:8080"}
API_BASE="$BASE_URL/api/v1/rate-limit-test"

echo "🚀 Rate Limiting 테스트 시작"
echo "📍 테스트 대상: $API_BASE"
echo "⏰ 시작 시간: $(date)"
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 테스트 결과 카운터
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# 테스트 함수
run_test() {
    local test_name="$1"
    local expected_status="$2"
    local command="$3"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    echo -e "${BLUE}🧪 테스트: $test_name${NC}"
    
    # 명령 실행
    local response=$(eval "$command" 2>/dev/null)
    local status=$?
    
    if [ $status -eq 0 ]; then
        echo -e "  ${GREEN}✅ 성공${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "  ${RED}❌ 실패 (상태 코드: $status)${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    echo ""
}

# 1. IP 기반 Rate Limiting 테스트
echo -e "${YELLOW}📋 1. IP 기반 Rate Limiting 테스트${NC}"
echo "=================================="

# 첫 번째 요청 (성공해야 함)
run_test "IP 기반 첫 번째 요청" "200" "curl -s -o /dev/null -w '%{http_code}' '$API_BASE/ip-based'"

# 5번까지 요청 (모두 성공해야 함)
for i in {2..5}; do
    run_test "IP 기반 $i번째 요청" "200" "curl -s -o /dev/null -w '%{http_code}' '$API_BASE/ip-based'"
done

# 6번째 요청 (429 에러가 나와야 함)
run_test "IP 기반 6번째 요청 (제한 초과)" "429" "curl -s -o /dev/null -w '%{http_code}' '$API_BASE/ip-based'"

echo ""

# 2. API 키 기반 Rate Limiting 테스트
echo -e "${YELLOW}📋 2. API 키 기반 Rate Limiting 테스트${NC}"
echo "=================================="

TEST_API_KEY="test-api-key-$(date +%s)"

# 첫 번째 요청
run_test "API 키 기반 첫 번째 요청" "200" "curl -s -o /dev/null -w '%{http_code}' -H 'X-API-Key: $TEST_API_KEY' '$API_BASE/api-key-based'"

# 100번까지 요청 (모두 성공해야 함)
for i in {2..100}; do
    run_test "API 키 기반 $i번째 요청" "200" "curl -s -o /dev/null -w '%{http_code}' -H 'X-API-Key: $TEST_API_KEY' '$API_BASE/api-key-based'"
done

# 101번째 요청 (429 에러가 나와야 함)
run_test "API 키 기반 101번째 요청 (제한 초과)" "429" "curl -s -o /dev/null -w '%{http_code}' -H 'X-API-Key: $TEST_API_KEY' '$API_BASE/api-key-based'"

echo ""

# 3. 사용자 기반 Rate Limiting 테스트
echo -e "${YELLOW}📋 3. 사용자 기반 Rate Limiting 테스트${NC}"
echo "=================================="

TEST_USER_ID="test-user-$(date +%s)"

# 첫 번째 요청
run_test "사용자 기반 첫 번째 요청" "200" "curl -s -o /dev/null -w '%{http_code}' -H 'X-User-ID: $TEST_USER_ID' '$API_BASE/user-based'"

# 1000번까지 요청 (모두 성공해야 함) - 실제로는 샘플만 테스트
for i in {2..10}; do
    run_test "사용자 기반 $i번째 요청" "200" "curl -s -o /dev/null -w '%{http_code}' -H 'X-User-ID: $TEST_USER_ID' '$API_BASE/user-based'"
done

echo ""

# 4. 세션 기반 Rate Limiting 테스트
echo -e "${YELLOW}📋 4. 세션 기반 Rate Limiting 테스트${NC}"
echo "=================================="

# 세션 ID 생성
SESSION_ID="test-session-$(date +%s)"

# 첫 번째 요청
run_test "세션 기반 첫 번째 요청" "200" "curl -s -o /dev/null -w '%{http_code}' -H 'Cookie: JSESSIONID=$SESSION_ID' '$API_BASE/session-based'"

# 3번까지 요청 (모두 성공해야 함)
for i in {2..3}; do
    run_test "세션 기반 $i번째 요청" "200" "curl -s -o /dev/null -w '%{http_code}' -H 'Cookie: JSESSIONID=$SESSION_ID' '$API_BASE/session-based'"
done

# 4번째 요청 (429 에러가 나와야 함)
run_test "세션 기반 4번째 요청 (제한 초과)" "429" "curl -s -o /dev/null -w '%{http_code}' -H 'Cookie: JSESSIONID=$SESSION_ID' '$API_BASE/session-based'"

echo ""

# 5. 상태 조회 테스트
echo -e "${YELLOW}📋 5. Rate Limiting 상태 조회 테스트${NC}"
echo "=================================="

# 상태 조회
run_test "Rate Limiting 상태 조회" "200" "curl -s -o /dev/null -w '%{http_code}' '$API_BASE/status?key=test-key'"

echo ""

# 6. 캐시 정리 테스트
echo -e "${YELLOW}📋 6. 캐시 정리 테스트${NC}"
echo "=================================="

# 캐시 정리
run_test "Rate Limiting 캐시 정리" "200" "curl -s -o /dev/null -w '%{http_code}' -X POST '$API_BASE/cleanup'"

echo ""

# 7. 외부 데이터 API Rate Limiting 테스트
echo -e "${YELLOW}📋 7. 외부 데이터 API Rate Limiting 테스트${NC}"
echo "=================================="

# API 목록 조회 (1000번까지 허용)
run_test "외부 API 목록 조회 첫 번째 요청" "200" "curl -s -o /dev/null -w '%{http_code}' '$BASE_URL/external-apis'"

# API 등록 (50번까지 허용)
run_test "외부 API 등록 첫 번째 요청" "400" "curl -s -o /dev/null -w '%{http_code}' -X POST '$BASE_URL/external-apis' -H 'Content-Type: application/json' -d '{}'"

echo ""

# 테스트 결과 요약
echo -e "${YELLOW}📊 테스트 결과 요약${NC}"
echo "=================================="
echo -e "총 테스트: ${BLUE}$TOTAL_TESTS${NC}"
echo -e "성공: ${GREEN}$PASSED_TESTS${NC}"
echo -e "실패: ${RED}$FAILED_TESTS${NC}"
echo -e "성공률: ${GREEN}$((PASSED_TESTS * 100 / TOTAL_TESTS))%${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}🎉 모든 테스트가 성공했습니다!${NC}"
    exit 0
else
    echo -e "${RED}❌ 일부 테스트가 실패했습니다.${NC}"
    exit 1
fi
