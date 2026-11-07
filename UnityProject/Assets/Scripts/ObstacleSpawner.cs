using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ObstacleSpawner : MonoBehaviour
{
    [SerializeField] private GameObject obstaclePrefab;
    [SerializeField] private RunnerController player;
    [SerializeField] private float spawnZ = 60f;
    [SerializeField] private float spawnY = 0.5f;
    [SerializeField] private float spawnInterval = 1.5f;
    [SerializeField] private float minimumInterval = 0.65f;
    [SerializeField] private float intervalAcceleration = 0.01f;

    private readonly List<int> previousLanes = new List<int>();

    private IEnumerator Start()
    {
        while (true)
        {
            SpawnObstacle();
            yield return new WaitForSeconds(spawnInterval);
            spawnInterval = Mathf.Max(minimumInterval, spawnInterval - intervalAcceleration);
        }
    }

    private void SpawnObstacle()
    {
        if (obstaclePrefab == null || player == null)
        {
            Debug.LogError("ObstacleSpawner: assign obstaclePrefab and player in Inspector.");
            return;
        }

        // Never block all three lanes: each spawn occupies exactly one lane.
        int lane = Random.Range(0, 3);
        if (previousLanes.Count > 0 && previousLanes[previousLanes.Count - 1] == lane)
        {
            List<int> alternatives = new List<int> { 0, 1, 2 };
            alternatives.Remove(lane);
            lane = alternatives[Random.Range(0, alternatives.Count)];
        }

        float x = (lane - 1) * 2f;
        GameObject obstacle = Instantiate(obstaclePrefab, new Vector3(x, spawnY, spawnZ), Quaternion.identity);
        ObstacleMover mover = obstacle.GetComponent<ObstacleMover>();
        if (mover == null) mover = obstacle.AddComponent<ObstacleMover>();
        mover.Initialize(player.ObstacleSpeed);

        previousLanes.Add(lane);
        if (previousLanes.Count > 4) previousLanes.RemoveAt(0);
    }
}
