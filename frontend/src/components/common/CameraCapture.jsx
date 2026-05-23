import { useEffect, useRef, useState } from 'react'
import { Camera, X } from 'lucide-react'

/**
 * Desktop webcam capture modal using getUserMedia.
 * Props:
 *   onCapture(base64: string) — called with JPEG data URI on snap
 *   onClose()                 — called when modal is dismissed
 */
export function CameraCapture({ onCapture, onClose }) {
  const videoRef = useRef(null)
  const streamRef = useRef(null)
  const [error, setError] = useState(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    let cancelled = false
    navigator.mediaDevices
      .getUserMedia({ video: { facingMode: 'user' }, audio: false })
      .then((stream) => {
        if (cancelled) { stream.getTracks().forEach((t) => t.stop()); return }
        streamRef.current = stream
        if (videoRef.current) {
          videoRef.current.srcObject = stream
          videoRef.current.onloadedmetadata = () => setReady(true)
        }
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Camera access denied')
      })
    return () => {
      cancelled = true
      streamRef.current?.getTracks().forEach((t) => t.stop())
    }
  }, [])

  function snap() {
    const video = videoRef.current
    if (!video) return
    const canvas = document.createElement('canvas')
    canvas.width = video.videoWidth
    canvas.height = video.videoHeight
    canvas.getContext('2d').drawImage(video, 0, 0)
    const base64 = canvas.toDataURL('image/jpeg', 0.85)
    streamRef.current?.getTracks().forEach((t) => t.stop())
    onCapture(base64)
  }

  function close() {
    streamRef.current?.getTracks().forEach((t) => t.stop())
    onClose()
  }

  return (
    <>
      <button
        type="button"
        aria-label="Close camera"
        className="fixed inset-0 z-[1500] bg-black/80"
        onClick={close}
      />
      <div className="fixed left-1/2 top-1/2 z-[1600] w-[min(560px,92vw)] -translate-x-1/2 -translate-y-1/2 overflow-hidden rounded-panel border border-border bg-surface shadow-xl">
        <div className="flex items-center justify-between px-4 py-3 border-b border-border">
          <span className="text-sm font-medium text-ink">Take photo</span>
          <button
            type="button"
            onClick={close}
            className="rounded-panel border border-border p-1.5 text-muted hover:bg-surface-alt hover:text-ink"
          >
            <X size={14} />
          </button>
        </div>

        <div className="relative bg-black">
          {error ? (
            <div className="flex h-48 items-center justify-center px-6 text-center text-sm text-red-400">
              {error}
            </div>
          ) : (
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className="w-full"
              style={{ maxHeight: '60vh', objectFit: 'cover' }}
            />
          )}
        </div>

        {!error && (
          <div className="flex justify-center px-4 py-3 border-t border-border">
            <button
              type="button"
              disabled={!ready}
              onClick={snap}
              className="flex items-center gap-2 rounded-panel border border-accent bg-accent px-5 py-2 text-sm text-white disabled:opacity-40"
            >
              <Camera size={15} />
              Snap
            </button>
          </div>
        )}
      </div>
    </>
  )
}
