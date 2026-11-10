
local count = redis.call('INCR', KEYS[1])

-- Initialize or restore the expiration if missing.
if count == 1 or redis.call('PTTL', KEYS[1]) < 0 then
    redis.call('PEXPIRE', KEYS[1], ARGV[1])
end

-- Reject requests exceeding the configured limit.
if count > tonumber(ARGV[2]) then
    return redis.call('PTTL', KEYS[1])
end

-- Zero means the request is allowed.
return 0
