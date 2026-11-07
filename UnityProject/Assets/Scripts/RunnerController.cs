using UnityEngine;

[RequireComponent(typeof(Rigidbody), typeof(BoxCollider))]
public class RunnerController : MonoBehaviour
{
    [SerializeField] private float laneMoveSpeed = 12f;
    [SerializeField] private float obstacleSpeed = 18f;
    [SerializeField] private int currentLane = 1;
    private Vector2 touchStart;
    private Rigidbody body;

    public int CurrentLane => currentLane;
    public float ObstacleSpeed => obstacleSpeed;

    private void Awake()
    {
        body = GetComponent<Rigidbody>();
        body.isKinematic = true;
        body.useGravity = false;
        transform.position = new Vector3(0f, 0.5f, 0f);
        transform.rotation = Quaternion.identity;
    }

    private void Update()
    {
        ReadSwipe();
        float targetX = (currentLane - 1) * 2f;
        Vector3 position = transform.position;
        position.x = Mathf.MoveTowards(position.x, targetX, laneMoveSpeed * Time.deltaTime);
        transform.position = position;
    }

    private void ReadSwipe()
    {
        if (Input.touchCount <= 0) return;
        Touch touch = Input.GetTouch(0);
        if (touch.phase == TouchPhase.Began)
        {
            touchStart = touch.position;
        }
        else if (touch.phase == TouchPhase.Ended)
        {
            float deltaX = touch.position.x - touchStart.x;
            if (Mathf.Abs(deltaX) >= 50f)
            {
                currentLane = Mathf.Clamp(currentLane + (deltaX > 0f ? 1 : -1), 0, 2);
            }
        }
    }

    public void SetObstacleSpeed(float value)
    {
        obstacleSpeed = Mathf.Max(0f, value);
    }

    private void OnCollisionEnter(Collision collision)
    {
        if (collision.collider.CompareTag("Obstacle"))
        {
            // Connect this event to the game's game-over flow.
            enabled = false;
        }
    }
}
