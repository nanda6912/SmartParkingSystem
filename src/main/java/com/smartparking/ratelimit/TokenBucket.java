package com.smartparking.ratelimit;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe Token Bucket implementation for rate limiting
 * Uses atomic operations and read-write locks for thread safety
 */
public class TokenBucket {
    
    private final long capacity;
    private final double refillRate;
    private final AtomicLong tokens;
    private volatile long lastRefillTime;
    private volatile long lastAccessTime;
    private final ReentrantReadWriteLock lock;
    
    /**
     * Create a new Token Bucket
     * @param capacity Maximum number of tokens (burst capacity)
     * @param refillRate Tokens added per second
     */
    public TokenBucket(long capacity, double refillRate) {
        if (capacity <= 0 || refillRate <= 0) {
            throw new IllegalArgumentException("Capacity and refill rate must be positive");
        }
        
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = new AtomicLong(capacity);
        this.lastRefillTime = System.currentTimeMillis();
        this.lastAccessTime = this.lastRefillTime;
        this.lock = new ReentrantReadWriteLock();
    }
    
    /**
     * Try to consume one token
     * @return true if token was consumed, false if rate limited
     */
    public boolean tryConsume() {
        lock.writeLock().lock();
        try {
            refill();
            lastAccessTime = System.currentTimeMillis();
            
            if (tokens.get() > 0) {
                tokens.decrementAndGet();
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Try to consume multiple tokens
     * @param tokenCount Number of tokens to consume
     * @return true if tokens were consumed, false if rate limited
     */
    public boolean tryConsume(long tokenCount) {
        if (tokenCount <= 0) {
            return false;
        }
        
        if (tokenCount > capacity) {
            return false; // Can't consume more than capacity
        }
        
        lock.writeLock().lock();
        try {
            refill();
            lastAccessTime = System.currentTimeMillis();
            
            if (tokens.get() >= tokenCount) {
                tokens.addAndGet(-tokenCount);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Refill tokens based on elapsed time
     */
    private void refill() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastRefillTime;
        
        if (timePassed > 0) {
            double tokensToAdd = (timePassed / 1000.0) * refillRate;
            long currentTokens = tokens.get();
            long newTokens = Math.min(capacity, currentTokens + (long) tokensToAdd);
            
            if (newTokens != currentTokens) {
                tokens.set(newTokens);
            }
            
            lastRefillTime = now;
        }
    }
    
    /**
     * Get current token count (for monitoring)
     * @return Current number of tokens
     */
    public long getAvailableTokens() {
        lock.readLock().lock();
        try {
            refill(); // Update tokens before returning count
            return tokens.get();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get time until next token is available
     * @return Milliseconds until next token, 0 if tokens available now
     */
    public long getTimeToNextToken() {
        lock.readLock().lock();
        try {
            refill();
            if (tokens.get() > 0) {
                return 0;
            }
            
            // Calculate time needed for 1 token
            return (long) (1000.0 / refillRate);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Get last access time (for cleanup)
     * @return Last access timestamp
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    /**
     * Get bucket capacity
     * @return Maximum token capacity
     */
    public long getCapacity() {
        return capacity;
    }
    
    /**
     * Get refill rate
     * @return Tokens per second
     */
    public double getRefillRate() {
        return refillRate;
    }
    
    @Override
    public String toString() {
        return String.format("TokenBucket{available=%d, capacity=%d, refillRate=%.2f}", 
                getAvailableTokens(), capacity, refillRate);
    }
}
