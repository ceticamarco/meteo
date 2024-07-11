from typing import Dict, Tuple
from datetime import datetime, timedelta

class Database:
    def __init__(self,  ttl=4):
        self.ttl = ttl # Time to live in hours
        self.tmp = 0
        self.cache: Dict[str, Tuple[Dict[str, str], datetime]] = dict() # key -> (value, date)

    def add_key(self, key: str, value: Dict[str, str]) -> None:
        ''' Given a key and a value, store it in the cache '''
        # Retrieve current datetime
        timestamp = datetime.now()
        # Prepare cached value
        cached_value = (value, timestamp)
        # Store value on the cache
        self.cache[key] = cached_value

    def get_key(self, key: str) -> Dict[str, str] | None:
        ''' Given a key, retrieve its value if and only if
            it exists in the cache and it is not expired '''
        # Check whether key exists in the cache
        if not self.is_cached(key): return None 

        # Otherwise retrieve value
        cached_value = self.cache[key]

        # Check whether key is expired
        timestamp = cached_value[1]
        current_time = datetime.now()
        offset = current_time - timedelta(hours=self.ttl)
        if timestamp < offset: return None

        # Otherwise, return the cached value
        return cached_value[0]

    def del_key(self, key: str) -> None:
        ''' Given a key, remove it regardless of whether
            it exists or not in the cache '''
        self.cache.pop(key, None)

    def is_cached(self, key: str) -> bool:
        ''' Given a key, returns True if it is cached,
            False otherwise '''
        
        return True if key in self.cache else False
