-- queue_check.lua
-- Pure-read status check with TTL refresh on metadata hashes.
-- Does NOT mutate queue state (no promotion/eviction).
--
-- KEYS[1] = queue:events:{eventId}:active
-- KEYS[2] = queue:events:{eventId}:waiting
-- KEYS[3] = queue:events:{eventId}:tokens
-- KEYS[4] = queue:events:{eventId}:users
--
-- ARGV[1] = token
-- ARGV[2] = metadataTtl (2 * activeWindowSec)
--
-- Returns: {phase, token, position, remainingSeconds}
--   phase: "ALLOWED", "WAITING", or "INVALID"

local activeKey  = KEYS[1]
local waitingKey = KEYS[2]
local tokensKey  = KEYS[3]
local usersKey   = KEYS[4]

local token       = ARGV[1]
local metadataTtl = tonumber(ARGV[2])

-- 1. Check active hash
local activeUserId = redis.call('HGET', activeKey, token)
if activeUserId then
    local pttlResult = redis.call('HPTTL', activeKey, 'FIELDS', 1, token)
    local remainingMs = pttlResult[1]
    local remainingSec = math.floor(remainingMs / 1000)
    -- Refresh metadata TTL
    redis.call('HEXPIRE', tokensKey, metadataTtl, 'FIELDS', 1, token)
    redis.call('HEXPIRE', usersKey, metadataTtl, 'FIELDS', 1, activeUserId)
    return {'ALLOWED', token, '0', tostring(remainingSec)}
end

-- 2. Check waiting ZSET
local rank = redis.call('ZRANK', waitingKey, token)
if rank ~= false then
    -- Get userId from tokens hash to refresh users hash TTL
    local metadata = redis.call('HGET', tokensKey, token)
    if metadata then
        local userId = string.match(metadata, '^([^:]+)')
        redis.call('HEXPIRE', tokensKey, metadataTtl, 'FIELDS', 1, token)
        redis.call('HEXPIRE', usersKey, metadataTtl, 'FIELDS', 1, userId)
    end
    return {'WAITING', token, tostring(rank + 1), '0'}
end

-- 3. Not found
return {'INVALID', '', '0', '0'}
