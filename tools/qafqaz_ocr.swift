import Foundation
import Vision
import CoreGraphics
import ImageIO

struct Observation {
    let minY: CGFloat
    let minX: CGFloat
    let text: String
}

guard CommandLine.arguments.count >= 2 else {
    FileHandle.standardError.write(Data("Usage: qafqaz_ocr.swift /absolute/path/to/image.png\n".utf8))
    exit(1)
}

let imagePath = CommandLine.arguments[1]
let imageUrl = URL(fileURLWithPath: imagePath) as CFURL

guard
    let source = CGImageSourceCreateWithURL(imageUrl, nil),
    let cgImage = CGImageSourceCreateImageAtIndex(source, 0, nil)
else {
    FileHandle.standardError.write(Data("Could not load image at \(imagePath)\n".utf8))
    exit(1)
}

let request = VNRecognizeTextRequest()
request.recognitionLevel = .accurate
request.usesLanguageCorrection = false
request.minimumTextHeight = 0.012
request.recognitionLanguages = ["az-AZ", "tr-TR", "en-US"]

let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
try handler.perform([request])

let observations = (request.results ?? [])
    .compactMap { observation -> Observation? in
        guard let candidate = observation.topCandidates(1).first else { return nil }
        let text = candidate.string
            .replacingOccurrences(of: "\t", with: " ")
            .replacingOccurrences(of: "\n", with: " ")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return nil }
        return Observation(
            minY: observation.boundingBox.minY,
            minX: observation.boundingBox.minX,
            text: text
        )
    }
    .sorted {
        if abs($0.minY - $1.minY) > 0.0005 {
            return $0.minY > $1.minY
        }
        return $0.minX < $1.minX
    }

for observation in observations {
    print(String(format: "%.4f\t%.4f\t%@", observation.minY, observation.minX, observation.text))
}
