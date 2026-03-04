-- queue_consume.lua
-- Consume an active token and promote the next waiting user.
--
-- KEYS[1] = queue:events:{eventId}:active
-- KEYS[2] = queue:events:{eventId}:waiting
-- KEYS[3] = queue:events:{eventId}:tokens
-- KEYS[4] = queue:events:{eventId}:users
--
-- ARGV[1] = token
-- ARGV[2] = activeWindowSec
--
-- Returns: 1 if consumed, 0 if invalid/expired

local activeKey  = KEYS[1]
local waitingKey = KEYS[2]
local tokensKey  = KEYS[3]
local usersKey   = KEYS[4]

local token           = ARGV[1]
local activeWindowSec = tonumber(ARGV[2])
local metadataTtl     = activeWindowSec * 2

-- 1. Verify token is in active hash (expired fields return nil)
local userId = redis.call('HGET', activeKey, token)
if not userId then
    return 0
end

-- 2. Remove consumed token
redis.call('HDEL', activeKey, token)
redis.call('HDEL', tokensKey, token)
redis.call('HDEL', usersKey, userId)

-- 3. Promote next waiting user
local candidates = redis.call('ZPOPMIN', waitingKey, 1)
if #candidates >= 2 then
    local nextToken = candidates[1]
    local nextMeta = redis.call('HGET', tokensKey, nextToken)
    if nextMeta then
        local nextUserId = string.match(nextMeta, '^([^:]+)')
        redis.call('HSET', activeKey, nextToken, nextUserId)
        redis.call('HEXPIRE', activeKey, activeWindowSec, 'FIELDS', 1, nextToken)
        -- Refresh metadata TTL for promoted user
        redis.call('HEXPIRE', tokensKey, metadataTtl, 'FIELDS', 1, nextToken)
        redis.call('HEXPIRE', usersKey, metadataTtl, 'FIELDS', 1, nextUserId)
    end
end

return 1
