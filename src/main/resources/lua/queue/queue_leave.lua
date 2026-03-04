-- queue_leave.lua
-- Remove a user from active (with promotion) or waiting queue.
--
-- KEYS[1] = queue:events:{eventId}:active
-- KEYS[2] = queue:events:{eventId}:waiting
-- KEYS[3] = queue:events:{eventId}:tokens
-- KEYS[4] = queue:events:{eventId}:users
--
-- ARGV[1] = token
-- ARGV[2] = activeWindowSec
--
-- Returns: 1 if removed, 0 if not found

local activeKey  = KEYS[1]
local waitingKey = KEYS[2]
local tokensKey  = KEYS[3]
local usersKey   = KEYS[4]

local token           = ARGV[1]
local activeWindowSec = tonumber(ARGV[2])
local metadataTtl     = activeWindowSec * 2

-- Get userId from tokens hash (may be nil if metadata expired)
local metadata = redis.call('HGET', tokensKey, token)
local userId = nil
if metadata then
    userId = string.match(metadata, '^([^:]+)')
end

-- 1. Try remove from active
local removedFromActive = redis.call('HDEL', activeKey, token)
if removedFromActive == 1 then
    if metadata then redis.call('HDEL', tokensKey, token) end
    if userId then redis.call('HDEL', usersKey, userId) end
    -- Promote next waiting user
    local candidates = redis.call('ZPOPMIN', waitingKey, 1)
    if #candidates >= 2 then
        local nextToken = candidates[1]
        local nextMeta = redis.call('HGET', tokensKey, nextToken)
        if nextMeta then
            local nextUserId = string.match(nextMeta, '^([^:]+)')
            redis.call('HSET', activeKey, nextToken, nextUserId)
            redis.call('HEXPIRE', activeKey, activeWindowSec, 'FIELDS', 1, nextToken)
            redis.call('HEXPIRE', tokensKey, metadataTtl, 'FIELDS', 1, nextToken)
            redis.call('HEXPIRE', usersKey, metadataTtl, 'FIELDS', 1, nextUserId)
        end
    end
    return 1
end

-- 2. Try remove from waiting
local removedFromWaiting = redis.call('ZREM', waitingKey, token)
if removedFromWaiting == 1 then
    if metadata then redis.call('HDEL', tokensKey, token) end
    if userId then redis.call('HDEL', usersKey, userId) end
    return 1
end

return 0
