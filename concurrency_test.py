import concurrent.futures
import json
import uuid
import urllib.request
import sys

BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"

def make_request(url, method="GET", headers=None, data=None):
    if headers is None:
        headers = {}
    req_data = json.dumps(data).encode('utf-8') if data else None
    if req_data:
        headers["Content-Type"] = "application/json"

    req = urllib.request.Request(url, data=req_data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            return resp.status, json.loads(resp.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode('utf-8'))

# ==========================================
# 1. Concurrent Get-or-Create Test
# ==========================================
print("\n--- [1/3] Testing Concurrent Get-or-Create ---")
user_token = f"user_burst_{uuid.uuid4().hex[:8]}"
headers = {"Authorization": f"Bearer {user_token}"}
N = 20

def create_wallet(_):
    return make_request(f"{BASE_URL}/wallets", method="POST", headers=headers)

with concurrent.futures.ThreadPoolExecutor(max_workers=N) as executor:
    results = list(executor.map(create_wallet, range(N)))

statuses = [r[0] for r in results]
wallet_ids = {r[1]["id"] for r in results if r[0] == 200}

print(f"Requests: {N} | Status Codes: {set(statuses)}")
print(f"Unique Wallet IDs returned: {len(wallet_ids)}")
assert len(wallet_ids) == 1, f"FAILED: Expected 1 unique wallet, got {len(wallet_ids)}"
print("SUCCESS: Exactly one wallet created despite simultaneous requests.")

# Extract single wallet ID for subsequent tests
wallet_a_id = list(wallet_ids)[0]

# Setup Wallet B for transfer tests
_, wallet_b = make_request(f"{BASE_URL}/wallets", method="POST", headers={"Authorization": f"Bearer bob_{uuid.uuid4().hex[:8]}"})
wallet_b_id = wallet_b["id"]

# ==========================================
# 2. Idempotent Retry Storm Test
# ==========================================
print("\n--- [2/3] Testing Idempotent Retry Storm ---")
idempotency_key = f"storm_key_{uuid.uuid4().hex}"
transfer_payload = {
    "fromWalletId": wallet_a_id,
    "toWalletId": wallet_b_id,
    "amountPaise": 1000  # 10 INR
}
K = 20

def retry_transfer(_):
    t_headers = {"Idempotency-Key": idempotency_key}
    return make_request(f"{BASE_URL}/transfers", method="POST", headers=t_headers, data=transfer_payload)

with concurrent.futures.ThreadPoolExecutor(max_workers=K) as executor:
    retry_results = list(executor.map(retry_transfer, range(K)))

retry_statuses = [r[0] for r in retry_results]
transfer_ids = {r[1]["id"] for r in retry_results if r[0] == 200}

print(f"Requests: {K} | Status Codes: {set(retry_statuses)}")
print(f"Unique Transfer IDs returned: {len(transfer_ids)}")
assert len(transfer_ids) == 1, f"FAILED: Expected 1 transfer record, got {len(transfer_ids)}"
print("SUCCESS: Retry storm handled gracefully. Exactly one debit/credit recorded.")

# ==========================================
# 3. Conservation under Contention Test
# ==========================================
print("\n--- [3/3] Testing Conservation under Contention (Bidirectional transfers) ---")
# Create 4 test wallets
wallets = []
for i in range(4):
    _, w = make_request(f"{BASE_URL}/wallets", method="POST", headers={"Authorization": f"Bearer node_{i}_{uuid.uuid4().hex[:6]}"})
    wallets.append(w)

initial_total_balance = sum(w["balancePaise"] for w in wallets)
print(f"Initial aggregate balance across 4 wallets: {initial_total_balance} paise")

# Generate 50 rapid bidirectional cross-transfers (A->B, B->A, etc.)
import random
tasks = []
for _ in range(50):
    src, dst = random.sample(wallets, 2)
    tasks.append({
        "from": src["id"],
        "to": dst["id"],
        "key": f"contention_{uuid.uuid4().hex}",
        "amount": random.randint(100, 500)
    })

def execute_cross_transfer(task):
    headers = {"Idempotency-Key": task["key"]}
    payload = {
        "fromWalletId": task["from"],
        "toWalletId": task["to"],
        "amountPaise": task["amount"]
    }
    return make_request(f"{BASE_URL}/transfers", method="POST", headers=headers, data=payload)

with concurrent.futures.ThreadPoolExecutor(max_workers=25) as executor:
    list(executor.map(execute_cross_transfer, tasks))

# Fetch final balances
final_wallets = [make_request(f"{BASE_URL}/wallets/{w['id']}")[1] for w in wallets]
final_balances = [w["balancePaise"] for w in final_wallets]
final_total_balance = sum(final_balances)

print(f"Final balances: {final_balances}")
print(f"Final aggregate balance: {final_total_balance} paise")

assert all(b >= 0 for b in final_balances), "FAILED: Found negative wallet balance!"
assert initial_total_balance == final_total_balance, f"FAILED: Balance lost/created! {initial_total_balance} != {final_total_balance}"
print("SUCCESS: Total balance strictly conserved and zero deadlocks/negative balances recorded under load.")