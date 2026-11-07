using UnityEngine;
public sealed class RunnerGame:MonoBehaviour{
 public static RunnerGame Instance{get;private set;} public bool IsPlaying{get;private set;} public int Score{get;private set;} public int Coins{get;private set;} public int Best{get;private set;} public float ObstacleSpeed=>Mathf.Min(30f,16f+Score*.012f);
 RunnerController player;float scoreT,coinT;string page="menu";int selected;readonly int[] prices={0,100,250,500,900};readonly string[] names={"AQUA","RUBY","GOLD","VIOLET","LIME"};
 void Awake(){if(Instance&&Instance!=this){Destroy(gameObject);return;}Instance=this;Coins=PlayerPrefs.GetInt("coins",0);Best=PlayerPrefs.GetInt("best",0);selected=PlayerPrefs.GetInt("selected",0);}
 public void SetPlayer(RunnerController p){player=p;}
 void Update(){if(!IsPlaying)return;scoreT+=Time.deltaTime;coinT+=Time.deltaTime;if(scoreT>=.1f){Score++;scoreT-=.1f;}if(coinT>=1f){Coins++;coinT-=1f;Save();}}
 public void StartRun(){foreach(var o in FindObjectsOfType<ObstacleMover>())Destroy(o.gameObject);Score=0;scoreT=coinT=0;player?.ResetRunner();page="playing";IsPlaying=true;}
 public void EndRun(){if(!IsPlaying)return;IsPlaying=false;page="over";if(player)player.InputEnabled=false;Best=Mathf.Max(Best,Score);Save();}
 void Save(){PlayerPrefs.SetInt("coins",Coins);PlayerPrefs.SetInt("best",Best);PlayerPrefs.SetInt("selected",selected);PlayerPrefs.Save();}
 void OnApplicationPause(bool p){if(p)Save();}void OnApplicationQuit(){Save();}
 void OnGUI(){GUI.skin.label.fontSize=Mathf.Max(18,Screen.width/23);GUI.skin.button.fontSize=Mathf.Max(18,Screen.width/24);float w=Screen.width,h=Screen.height;
 if(IsPlaying){GUI.color=new Color(.03f,.08f,.13f,.9f);GUI.Box(new Rect(10,10,w*.55f,100),"");GUI.color=Color.white;GUI.Label(new Rect(24,18,w*.5f,40),"SCORE "+Score);GUI.color=Color.yellow;GUI.Label(new Rect(24,58,w*.5f,40),"COINS "+Coins);GUI.color=Color.white;GUI.Label(new Rect(w*.7f,22,w*.28f,40),"BEST "+Best);return;}
 float pw=w*.84f,ph=h*.48f;Rect r=new Rect((w-pw)/2,(h-ph)/2,pw,ph);GUI.color=new Color(.02f,.06f,.11f,.94f);GUI.Box(r,"");GUI.color=Color.white;
 GUIStyle title=new GUIStyle(GUI.skin.label){fontSize=Mathf.Max(28,w/13),fontStyle=FontStyle.Bold,alignment=TextAnchor.MiddleCenter};GUI.Label(new Rect(r.x,r.y+15,r.width,60),"ROAD RUNNER 3D",title);
 GUI.Label(new Rect(r.x+10,r.y+80,r.width-20,45),"BEST "+Best+"     COINS "+Coins);
 if(page=="shop"){for(int i=0;i<5;i++){bool own=PlayerPrefs.GetInt("car"+i,i==0?1:0)==1;string label=names[i]+"  "+(own?(selected==i?"SELECTED":"SELECT"):(prices[i]+" COINS"));if(GUI.Button(new Rect(r.x+15,r.y+125+i*48,r.width-30,42),label)){if(own)selected=i;else if(Coins>=prices[i]){Coins-=prices[i];PlayerPrefs.SetInt("car"+i,1);selected=i;}Save();}}if(GUI.Button(new Rect(r.x+20,r.y+r.height-50,r.width-40,40),"BACK"))page="menu";}
 else{GUI.Label(new Rect(r.x+10,r.y+130,r.width-20,45),page=="over"?"CRASH! SCORE "+Score:"DODGE TRAFFIC • COLLECT COINS");if(GUI.Button(new Rect(r.x+25,r.y+190,r.width-50,58),page=="over"?"PLAY AGAIN":"START RUN"))StartRun();if(GUI.Button(new Rect(r.x+25,r.y+260,r.width-50,58),"CAR SHOP"))page="shop";}}
}