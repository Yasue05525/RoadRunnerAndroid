using UnityEngine;
public sealed class ObstacleSpawner:MonoBehaviour{
 [SerializeField] GameObject prefab;[SerializeField] Transform player;float timer;int lastLane=-1;
 public void Configure(GameObject p,Transform target){prefab=p;player=target;}
 void Update(){if(RunnerGame.Instance==null||!RunnerGame.Instance.IsPlaying)return;timer-=Time.deltaTime;if(timer>0)return;Spawn();timer=Mathf.Max(.65f,1.5f-RunnerGame.Instance.Score/1200f);}
 void Spawn(){if(!prefab||!player)return;int lane=Random.Range(0,3);if(lane==lastLane)lane=(lane+Random.Range(1,3))%3;lastLane=lane;var go=Instantiate(prefab,new Vector3((lane-1)*2f,.5f,60f),Quaternion.identity);go.GetComponent<ObstacleMover>().Initialize(RunnerGame.Instance.ObstacleSpeed);}
}