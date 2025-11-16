#!/bin/bash

echo "🚀 Starting HAPI FHIR with Partition Fix"
echo "========================================"
echo ""

# Kill any existing HAPI FHIR process
echo "Stopping any existing HAPI FHIR instances..."
pkill -f "hapi-fhir-jpaserver" || true
sleep 2

# Start HAPI FHIR
echo "Starting HAPI FHIR server..."
mvn spring-boot:run > /tmp/hapi-fhir.log 2>&1 &
HAPI_PID=$!

echo "HAPI FHIR starting... (PID: $HAPI_PID)"
echo "Logs: tail -f /tmp/hapi-fhir.log"
echo ""
echo "Waiting for server to be ready (max 3 minutes)..."

# Wait for server to start
for i in {1..36}; do
    sleep 5
    if curl -s http://localhost:8080/fhir/metadata > /dev/null 2>&1; then
        echo ""
        echo "✅ HAPI FHIR is ready!"
        echo "   URL: http://localhost:8080/fhir"
        echo "   Swagger: http://localhost:8080/fhir/swagger-ui/"
        echo ""
        echo "Test it:"
        echo "  curl http://localhost:8080/fhir/metadata | jq '.fhirVersion'"
        exit 0
    fi
    
    # Check for errors in log
    if grep -q "HAPI-1220" /tmp/hapi-fhir.log; then
        echo ""
        echo "❌ HAPI-1220 partition error detected!"
        echo "Fix not applied correctly. Check /tmp/hapi-fhir.log"
        tail -50 /tmp/hapi-fhir.log
        exit 1
    fi
    
    echo -n "."
done

echo ""
echo "❌ HAPI FHIR failed to start within 3 minutes"
echo "Check logs: tail -f /tmp/hapi-fhir.log"
exit 1
