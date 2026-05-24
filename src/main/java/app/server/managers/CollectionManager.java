package app.server.managers;

import app.common.models.Movie;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class CollectionManager {
    private TreeMap<Long, Movie> collection;
    private final Map<Long, String> owners;
    private final ZonedDateTime initializationDate;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public CollectionManager() {
        this.collection = new TreeMap<>();
        this.owners = new HashMap<>();
        this.initializationDate = ZonedDateTime.now();
    }

    public void setCollection(TreeMap<Long, Movie> collection, Map<Long, String> owners) {
        lock.writeLock().lock();
        try {
            this.collection = collection;
            this.owners.clear();
            this.owners.putAll(owners);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ZonedDateTime getInitializationDate() {
        return initializationDate;
    }

    public String getOwnerByKey(Long key) {
        lock.readLock().lock();
        try {
            return owners.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getSize() {
        lock.readLock().lock();
        try {
            return collection.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String getCollectionType() {
        lock.readLock().lock();
        try {
            return collection.getClass().getSimpleName();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsKey(Long key) {
        lock.readLock().lock();
        try {
            return collection.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Movie getMovieByKey(Long key) {
        lock.readLock().lock();
        try {
            return collection.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Long getKeyById(Long id) {
        lock.readLock().lock();
        try {
            return collection.entrySet().stream()
                    .filter(e -> e.getValue().getId().equals(id))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Movie> getSortedByLocation() {
        lock.readLock().lock();
        try {
            return collection.values().stream()
                    .sorted((m1, m2) -> {
                        if (m1.getOperator() == null || m1.getOperator().getLocation() == null)
                            return 1;
                        if (m2.getOperator() == null || m2.getOperator().getLocation() == null)
                            return -1;
                        Float locX1 = m1.getOperator().getLocation().getX();
                        Float locX2 = m2.getOperator().getLocation().getX();
                        return Float.compare(locX1, locX2);
                    })
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void insertIntoMemory(Long key, Movie movie, String ownerLogin) {
        lock.writeLock().lock();
        try {
            collection.put(key, movie);
            owners.put(key, ownerLogin);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean removeKeyFromMemory(Long key) {
        lock.writeLock().lock();
        try {
            boolean existed = collection.containsKey(key);
            collection.remove(key);
            owners.remove(key);
            return existed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clearOwnerFromMemory(String ownerLogin) {
        lock.writeLock().lock();
        try {
            Set<Long> keysToRemove = owners.entrySet().stream()
                    .filter(e -> ownerLogin.equals(e.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
            keysToRemove.forEach(k -> {
                collection.remove(k);
                owners.remove(k);
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean updateInMemory(Long id, Movie newMovie) {
        lock.writeLock().lock();
        try {
            Long key = collection.entrySet().stream()
                    .filter(entry -> entry.getValue().getId().equals(id))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
            if (key != null) {
                newMovie.setId(id);
                newMovie.setCreationDate(collection.get(key).getCreationDate());
                collection.put(key, newMovie);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void forceReplaceInMemory(Long key, Movie newMovie) {
        lock.writeLock().lock();
        try {
            collection.put(key, newMovie);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Long> getKeysLowerThan(Movie compareMovie, String ownerLogin) {
        lock.readLock().lock();
        try {
            return collection.entrySet().stream()
                    .filter(e -> ownerLogin.equals(owners.get(e.getKey())))
                    .filter(e -> e.getValue().compareTo(compareMovie) < 0)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void removeLowerFromMemory(List<Long> keys) {
        lock.writeLock().lock();
        try {
            keys.forEach(k -> {
                collection.remove(k);
                owners.remove(k);
            });
        } finally {
            lock.writeLock().unlock();
        }
    }

    public double getAverageOscarsCount() {
        lock.readLock().lock();
        try {
            return collection.values().stream()
                    .mapToLong(Movie::getOscarsCount)
                    .average()
                    .orElse(0.0);
        } finally {
            lock.readLock().unlock();
        }
    }

    public long countLessThanLength(int length) {
        lock.readLock().lock();
        try {
            return collection.values().stream()
                    .filter(movie -> movie.getLength() < length)
                    .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Integer> getFieldDescendingLength() {
        lock.readLock().lock();
        try {
            return collection.values().stream()
                    .map(Movie::getLength)
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
}