using UnityEngine;

[RequireComponent(typeof(BoxCollider), typeof(Rigidbody))]
public class ObstacleMover : MonoBehaviour
{
    private float speed;

    public void Initialize(float moveSpeed)
    {
        speed = Mathf.Max(0f, moveSpeed);
    }

    private void Update()
    {
        transform.position += Vector3.back * speed * Time.deltaTime;
        if (transform.position.z < -20f)
            Destroy(gameObject);
    }
}
