#include "ThreadSafeSharedObject.h"

#include <array>
#include <atomic>
#include <barrier>
#include <cstddef>
#include <iostream>
#include <memory>
#include <thread>
#include <vector>

namespace {

struct Value final {
  explicit Value(std::size_t round) : round(round) {}

  std::size_t round;
};

constexpr std::size_t kRounds = 256;
constexpr std::size_t kThreadCount = 16;

} // namespace

int main() {
  using Holder = rnscreens::ThreadSafeSharedObject<Value>;

  std::array<std::unique_ptr<Holder>, kRounds> holders;
  std::array<std::atomic<std::size_t>, kRounds> constructionCounts{};
  std::array<std::array<std::shared_ptr<Value>, kThreadCount>, kRounds> results;
  std::barrier startRound(static_cast<std::ptrdiff_t>(kThreadCount));
  std::barrier finishRound(static_cast<std::ptrdiff_t>(kThreadCount));

  for (auto &holder : holders) {
    holder = std::make_unique<Holder>();
  }

  std::vector<std::thread> threads;
  threads.reserve(kThreadCount);
  for (std::size_t thread = 0; thread < kThreadCount; ++thread) {
    threads.emplace_back([&, thread] {
      for (std::size_t round = 0; round < kRounds; ++round) {
        startRound.arrive_and_wait();
        results[round][thread] = holders[round]->getOrCreate([&, round] {
          constructionCounts[round].fetch_add(1, std::memory_order_relaxed);
          std::this_thread::yield();
          return std::make_shared<Value>(round);
        });
        finishRound.arrive_and_wait();
      }
    });
  }

  for (auto &thread : threads) {
    thread.join();
  }

  for (std::size_t round = 0; round < kRounds; ++round) {
    const auto &expected = results[round][0];
    if (!expected || expected->round != round ||
        constructionCounts[round].load(std::memory_order_relaxed) != 1) {
      std::cerr << "Invalid construction in round " << round << '\n';
      return 1;
    }

    for (const auto &result : results[round]) {
      const bool sameOwner =
          !expected.owner_before(result) && !result.owner_before(expected);
      if (result.get() != expected.get() || !sameOwner) {
        std::cerr << "Mismatched shared object in round " << round << '\n';
        return 1;
      }
    }
  }

  return 0;
}
