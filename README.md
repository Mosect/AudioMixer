# AudioMixer
Android音频混合库

# 指引

## 1. 引用项目

[![](https://jitpack.io/v/Mosect/AudioMixer.svg)](https://jitpack.io/#Mosect/AudioMixer) <--- 点击查看

## 2. 示例
AudioMixer所有操作都是线程安全的，无需考虑线程安全问题；AudioMixer释放后会自动释放其他有关资源，无需再对其他相关对象进行释放

### 2.1 创建AudioMixer对象

```
// sampleRate: 采样率
// channelCount: 声道数量
// timeLength: 时间长度，单位：微秒（注意：这里不是毫秒），表示一次混合的音频时间
// pcmType: pcm格式类型：PcmType.BIT16、PcmType.BIT8，目前只支持两种
AudioMixer audioMixer = new AudioMixer(sampleRate, channelCount, timeLength, pcmType);
```

### 2.2 处理音频混合数据

建立线程，在线程循环中执行以下代码：
```
while(loop) {
  // 阻塞方式
  ByteBuffer buffer = audioMixer.tickAndWait();
  // 非阻塞方式
  // ByteBuffer buffer = mixer.tick();
  if (null != buffer) {
    // buffer里包含了混合后的声音，读取此buffer即可
    // ...
    
    
    // 数据处理后，需要解锁buffer，不执行会导致其输入音频会一直阻塞
    audioMixer.unlock();
  }
}
```

### 2.3 创建或删除输入音轨

AudioMixer可以创建多个输入音轨进行混合，以下为音轨创建示例：
```
// sampleRate: 采样率
// channelCount: 声道数量
// pcmType: pcm格式类型：PcmType.BIT16、PcmType.BIT8，目前只支持两种
// 请求创建的输入音轨事件长度同AudioMixer本身时间长度；
// 可以创建与AudioMixer本身不同的采样率、声道数量、PcmType进行声音混合
AudioTrack audioTrack = audioMixer.requestTrack(sampleRate, channelCount, pcmType);

// 不再需要用此音轨时，可以删除
audioTrack.delete();
```

### 2.4 向输入音轨写入数据

使用ByteBuffer作为数据源，写入数据
```
// data为ByteBuffer类型，其position和limit要提前设置号
public static void writeByteBufferToTrack(AudioTrack track, ByteBuffer data) {
  int size = data.remaining();
  int offset = data.position();
  int limit = data.limit();
  int writeLen = 0;
  while (writeLen < size) {
      data.position(writeLen + offset);
      data.limit(limit);
      int len = track.write(data);
      if (len < 0) {
          // 返回负数，特殊值，需要进行判断
          if (len == AudioTrack.WRITE_RESULT_FULL) {
              // 已填满
              track.flush();
              // 等待buffer解锁
              track.waitUnlock();
          } else {
              // 发生错误
              break;
          }
      } else {
          writeLen += len;
      }
  }
}
```

使用字节数据作为数据源，写入数据
```
public static void writeBytesToTrack(AudioTrack track, byte[] data, int offset, int size) {
  int writeLen = 0;
  while (writeLen < size) {
      int len = track.write(data, offset + writeLen, size - writeLen);
      if (len < 0) {
          // 返回负数，特殊值，需要进行判断
          if (len == AudioTrack.WRITE_RESULT_FULL) {
              // 已填满
              track.flush();
              // 等待buffer解锁
              track.waitUnlock();
          } else {
              // 发生错误，可以在此判断错误类型
              // AudioTrack.WRITE_RESULT_LOCKED
              // AudioTrack.WRITE_RESULT_DELETED
              break;
          }
      } else {
          writeLen += len;
      }
  }
}
```

### 2.5 释放AudioMixer
不再使用AudioMixer时，进行释放

```
audioMixer.release();
// 如果有线程再循环，需要自己去关闭线程循环，AudioMixer没有管理线程的能力
// loop = false;
```

# 其他

更详细使用请查看demo
