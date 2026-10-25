package com.farika.soundflow;
final class EqPresets {
 static final String[] NAMES={"Ровный","Бас","Рок","Поп","Вокал","Электроника","Классика"};
 static final int[][] GAINS={{0,0,0,0,0},{600,350,0,-100,100},{400,200,-100,300,450},{-100,200,350,200,100},{-300,-100,450,300,-100},{500,250,-150,250,500},{200,100,-100,150,300}};
 static int[] gains(int index){return GAINS[Math.max(0,Math.min(GAINS.length-1,index))].clone();}
}
