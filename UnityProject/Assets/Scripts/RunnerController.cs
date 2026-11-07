using UnityEngine;
[RequireComponent(typeof(Rigidbody),typeof(BoxCollider))]
public sealed class RunnerController:MonoBehaviour{
 [SerializeField] float laneSpeed=14f,swipePixels=50f; int lane=1; Vector2 touchStart; Rigidbody body; public bool InputEnabled{get;set;}=true;
 void Awake(){body=GetComponent<Rigidbody>();body.isKinematic=true;body.useGravity=false;body.interpolation=RigidbodyInterpolation.Interpolate;}
 void Update(){if(!InputEnabled)return;if(Input.touchCount>0){Touch t=Input.GetTouch(0);if(t.phase==TouchPhase.Began)touchStart=t.position;else if(t.phase==TouchPhase.Ended){float dx=t.position.x-touchStart.x;if(Mathf.Abs(dx)>=swipePixels)lane=Mathf.Clamp(lane+(dx>0?1:-1),0,2);}}
 #if UNITY_EDITOR || UNITY_STANDALONE
 if(Input.GetKeyDown(KeyCode.LeftArrow)||Input.GetKeyDown(KeyCode.A))lane=Mathf.Max(0,lane-1);if(Input.GetKeyDown(KeyCode.RightArrow)||Input.GetKeyDown(KeyCode.D))lane=Mathf.Min(2,lane+1);
 #endif
 Vector3 p=body.position;p.x=Mathf.MoveTowards(p.x,(lane-1)*2f,laneSpeed*Time.deltaTime);body.MovePosition(p);}
 public void ResetRunner(){lane=1;body.position=new Vector3(0,.5f,0);InputEnabled=true;}
 void OnCollisionEnter(Collision c){if(c.collider.CompareTag("Obstacle"))RunnerGame.Instance?.EndRun();}
}