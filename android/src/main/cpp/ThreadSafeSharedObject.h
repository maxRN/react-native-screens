#pragma once

#include <memory>
#include <mutex>
#include <utility>

namespace rnscreens {

template <typename T>
class ThreadSafeSharedObject final {
 public:
  template <typename Factory>
  std::shared_ptr<T> getOrCreate(Factory &&factory) {
    std::lock_guard<std::mutex> lock(mutex_);

    if (!object_) {
      object_ = std::forward<Factory>(factory)();
    }

    return object_;
  }

 private:
  std::mutex mutex_;
  std::shared_ptr<T> object_;
};

} // namespace rnscreens
