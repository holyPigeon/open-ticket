-- queue_promote.lua
-- Scheduler batch promotion: fill freed active slots from waiting queue.
--
-- KEYS[1] = queue:events:{eventId}:active
-- KEYS[2] = queue:events:{eventId}:waiting
-- KEYS[3] = queue:events:{eventId}:tokens
-- KEYS[4] = queue:events:{eventId}:users
-- KEYS[5] = queue:events:registry
--
-- ARGV[1] = maxActive
-- ARGV[2] = activeWindowSec
-- ARGV[3] = eventId
--
-- Returns: number of promoted users

local activeKey   = KEYS[1]
local waitingKey  = KEYS[2]
local tokensKey   = KEYS[3]
local usersKey    = KEYS[4]
local registryKey = KEYS[5]

local maxActive       = tonumber(ARGV[1])
local activeWindowSec = tonumber(ARGV[2])
local eventId         = ARGV[3]
local metadataTtl     = activeWindowSec * 2

-- Early exit: no waiting users
local waitingCount = redis.call('ZCARD', waitingKey)
if waitingCount == 0 then
    -- Clean up registry if active is also empty
    local activeCount = redis.call('HLEN', activeKey)
    if activeCount == 0 then
        redis.call('SREM', registryKey, eventId)
    end
    return 0
end

-- Calculate free slots
local activeCount = redis.call('HLEN', activeKey)
local slotsAvailable = maxActive - activeCount
if slotsAvailable <= 0 then
    return 0
end

-- Batch promote
local candidates = redis.call('ZPOPMIN', waitingKey, slotsAvailable)
local promoted = 0
for i = 1, #candidates, 2 do
    local promoteToken = candidates[i]
    local meta = redis.call('HGET', tokensKey, promoteToken)
    if meta then
        local promoteUserId = string.match(meta, '^([^:]+)')
        redis.call('HSET', activeKey, promoteToken, promoteUserId)
        redis.call('HEXPIRE', activeKey, activeWindowSec, 'FIELDS', 1, promoteToken)
        redis.call('HEXPIRE', tokensKey, metadataTtl, 'FIELDS', 1, promoteToken)
        redis.call('HEXPIRE', usersKey, metadataTtl, 'FIELDS', 1, promoteUserId)
        promoted = promoted + 1
    end
end

-- Clean up registry if both queues empty
if redis.call('HLEN', activeKey) == 0 and redis.call('ZCARD', waitingKey) == 0 then
    redis.call('SREM', registryKey, eventId)
end

return promoted
