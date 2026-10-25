package com.farika.soundflow;
/** Recent system-service restart only; opening UI is never an autoplay request. */
final class RestartPolicy {
 static boolean resume(boolean wanted,boolean suspended,boolean enabled,long savedAt,long now){return enabled&&wanted&&!suspended&&savedAt>0&&now>=savedAt&&now-savedAt<=120000;}
}
