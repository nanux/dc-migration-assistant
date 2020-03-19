mvn amps:run &
sleep 300

STATUS_CODE=$()curl -s -o /dev/null -w "%{http_code}" -u admin:admin http://localhost:2990/jira/rest/dc-migration/1.0/migration)
