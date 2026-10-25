# Account writes in Lab0.7

The session token and verified client configuration are now stored as a pair, not mixed with a newly refreshed hourly public-client configuration. Upgrade migration can only preserve the last known client; it cannot recover an already lost original association. Login verification saves the actual client used for the verified/me request.

This is a candidate fix for client mismatch, not proof that real403 errors are solved. A/me response does not prove write authorization. READ_ACCOUNT versus WRITE and ACCOUNT_ACTION_STATUS distinguish where a refusal happened, without exposing tokens or cookies.

Official OAuth requirements: https://developers.soundcloud.com/docs/api/guide.html . The current experimental client does not contain registered OAuth app credentials. A native mutation that SoundCloud rejects remains rejected; no hidden browser or false local cloud-success fallback is used.

The user must not share passwords, cookies or access tokens in chat. A screenshot of the safe action/stage/status code is enough for further diagnosis. Real-account acceptance was not tested in this build environment.
