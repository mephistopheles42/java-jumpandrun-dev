#!/bin/bash

java -Djava.library.path=lib_nativ -XX:+UnlockExperimentalVMOptions -XX:+DoEscapeAnalysis -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:MaxGCPauseMillis=2 -XX:+AggressiveOpts -XX:+UseBiasedLocking -XX:+AlwaysPreTouch -XX:ParallelGCThreads=2 -Xmx1g -Xms128m -XX:NewRatio=2 -jar game.jar 
