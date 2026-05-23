import { useRef, useState } from 'react'
import { Camera, Upload, X } from 'lucide-react'
import { compressToBase64 } from '../../utils/imageUtils'
import { CameraCapture } from './CameraCapture'

const isMobile = typeof navigator !== 'undefined' && navigator.maxTouchPoints > 0

/**
 * Props:
 *   photos    – string[]  (base64 data URIs)
 *   onChange  – (photos: string[]) => void
 */
export function PhotoUpload({ photos = [], onChange }) {
  const fileInputRef = useRef(null)
  const mobileInputRef = useRef(null)
  const [showCamera, setShowCamera] = useState(false)

  async function handleFiles(files) {
    if (!files?.length) return
    const compressed = await Promise.all(
      Array.from(files).map((f) => compressToBase64(f))
    )
    onChange([...photos, ...compressed])
  }

  function handleCapture(base64) {
    onChange([...photos, base64])
    setShowCamera(false)
  }

  function remove(index) {
    onChange(photos.filter((_, i) => i !== index))
  }

  return (
    <div className="space-y-2">
      <div className="flex gap-2">
        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          className="flex items-center gap-2 rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink hover:bg-surface-alt"
        >
          <Upload size={15} />
          Upload photo
        </button>

        {isMobile ? (
          <button
            type="button"
            onClick={() => mobileInputRef.current?.click()}
            className="flex items-center gap-2 rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink hover:bg-surface-alt"
          >
            <Camera size={15} />
            Take photo
          </button>
        ) : (
          <button
            type="button"
            onClick={() => setShowCamera(true)}
            className="flex items-center gap-2 rounded-panel border border-border bg-surface px-3 py-2 text-sm text-ink hover:bg-surface-alt"
          >
            <Camera size={15} />
            Take photo
          </button>
        )}
      </div>

      {/* File picker — multiple files, no capture */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        multiple
        className="hidden"
        onChange={(e) => handleFiles(e.target.files)}
      />

      {/* Mobile: native camera via capture attribute */}
      <input
        ref={mobileInputRef}
        type="file"
        accept="image/*"
        capture="environment"
        className="hidden"
        onChange={(e) => handleFiles(e.target.files)}
      />

      {photos.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {photos.map((src, i) => (
            <div key={i} className="relative">
              <img
                src={src}
                alt={`Photo ${i + 1}`}
                className="h-20 w-20 rounded-panel border border-border object-cover"
              />
              <button
                type="button"
                onClick={() => remove(i)}
                className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full border border-border bg-surface text-muted hover:text-ink"
                aria-label="Remove photo"
              >
                <X size={11} />
              </button>
            </div>
          ))}
        </div>
      )}

      {showCamera && (
        <CameraCapture
          onCapture={handleCapture}
          onClose={() => setShowCamera(false)}
        />
      )}
    </div>
  )
}
