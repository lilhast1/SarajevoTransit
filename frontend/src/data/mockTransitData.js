export const sarajevoCenter = [43.8563, 18.4131]

export const vehicleTypes = [
  { id: 1, name: 'minibus' },
  { id: 2, name: 'bus' },
  { id: 3, name: 'trolleybus' },
  { id: 4, name: 'tram' },
]

export const lines = [
  { id: 1, code: '3', name: 'Ilidza - Bascarsija', vehicleTypeId: 4, vehicleTypeName: 'tram', isActive: true },
  { id: 2, code: '31e', name: 'Dobrinja - Vijecnica', vehicleTypeId: 2, vehicleTypeName: 'bus', isActive: true },
  { id: 3, code: '103', name: 'Trg Austrije - Dobrinja', vehicleTypeId: 3, vehicleTypeName: 'trolleybus', isActive: true },
  { id: 4, code: '55', name: 'Drvenija - Sedrenik', vehicleTypeId: 1, vehicleTypeName: 'minibus', isActive: true },
]

export const directions = [
  { id: 11, lineId: 1, lineName: 'Ilidza - Bascarsija', directionLabel: 'A->B', name: 'Ilidza to Bascarsija', isActive: true },
  { id: 12, lineId: 1, lineName: 'Ilidza - Bascarsija', directionLabel: 'B->A', name: 'Bascarsija to Ilidza', isActive: true },
  { id: 21, lineId: 2, lineName: 'Dobrinja - Vijecnica', directionLabel: 'A->B', name: 'Dobrinja to Vijecnica', isActive: true },
  { id: 22, lineId: 2, lineName: 'Dobrinja - Vijecnica', directionLabel: 'B->A', name: 'Vijecnica to Dobrinja', isActive: true },
  { id: 31, lineId: 3, lineName: 'Trg Austrije - Dobrinja', directionLabel: 'A->B', name: 'Trg Austrije to Dobrinja', isActive: true },
  { id: 32, lineId: 3, lineName: 'Trg Austrije - Dobrinja', directionLabel: 'B->A', name: 'Dobrinja to Trg Austrije', isActive: true },
]

export const stations = [
  { id: 101, name: 'Ilidza Terminal', address: 'Rustempasina', latitude: 43.8299, longitude: 18.3048, isActive: true },
  { id: 102, name: 'Stup', address: 'Bulevar Meše Selimovića', latitude: 43.8452, longitude: 18.3356, isActive: true },
  { id: 103, name: 'Otoka', address: 'Dzemala Bijedica', latitude: 43.8488, longitude: 18.3697, isActive: true },
  { id: 104, name: 'Skenderija', address: 'Terezija', latitude: 43.8544, longitude: 18.4147, isActive: true },
  { id: 105, name: 'Bascarsija', address: 'Telali', latitude: 43.8596, longitude: 18.4333, isActive: true },
  { id: 106, name: 'Dobrinja', address: 'Trg djece Dobrinje', latitude: 43.8244, longitude: 18.3431, isActive: true },
  { id: 107, name: 'Vijecnica', address: 'Obala Kulina bana', latitude: 43.8592, longitude: 18.4345, isActive: true },
  { id: 108, name: 'Trg Austrije', address: 'Bistrik', latitude: 43.8497, longitude: 18.4211, isActive: true },
]

export const directionStations = {
  11: [101, 102, 103, 104, 105],
  12: [105, 104, 103, 102, 101],
  21: [106, 103, 104, 107],
  22: [107, 104, 103, 106],
  31: [108, 104, 103, 106],
  32: [106, 103, 104, 108],
}

export const routePolylines = {
  11: [
    [43.8299, 18.3048],
    [43.8391, 18.324],
    [43.8452, 18.3356],
    [43.8488, 18.3697],
    [43.8544, 18.4147],
    [43.8596, 18.4333],
  ],
  21: [
    [43.8244, 18.3431],
    [43.8399, 18.3582],
    [43.8488, 18.3697],
    [43.8544, 18.4147],
    [43.8592, 18.4345],
  ],
  31: [
    [43.8497, 18.4211],
    [43.8544, 18.4147],
    [43.8488, 18.3697],
    [43.8244, 18.3431],
  ],
}

export const timetables = [
  { id: 1, lineId: 1, directionId: 11, departureTime: '06:00', daysOfWeek: [1, 2, 3, 4, 5], isActive: true },
  { id: 2, lineId: 1, directionId: 11, departureTime: '06:15', daysOfWeek: [1, 2, 3, 4, 5], isActive: true },
  { id: 3, lineId: 1, directionId: 11, departureTime: '06:30', daysOfWeek: [1, 2, 3, 4, 5], isActive: true },
  { id: 4, lineId: 1, directionId: 11, departureTime: '07:00', daysOfWeek: [6], isActive: true },
  { id: 5, lineId: 1, directionId: 11, departureTime: '08:00', daysOfWeek: [7], isActive: true },
  { id: 6, lineId: 1, directionId: 12, departureTime: '06:10', daysOfWeek: [1, 2, 3, 4, 5], isActive: true },
  { id: 7, lineId: 2, directionId: 21, departureTime: '06:05', daysOfWeek: [1, 2, 3, 4, 5], isActive: true },
  { id: 8, lineId: 2, directionId: 22, departureTime: '06:25', daysOfWeek: [1, 2, 3, 4, 5], isActive: true },
  { id: 9, lineId: 3, directionId: 31, departureTime: '06:20', daysOfWeek: [1, 2, 3, 4, 5], isActive: true },
  { id: 10, lineId: 3, directionId: 32, departureTime: '06:40', daysOfWeek: [1, 2, 3, 4, 5], isActive: true },
]

export const mockUsers = [
  {
    id: 1,
    fullName: 'Demo User',
    email: 'demo@sarajevotransit.ba',
    password: 'Password123',
  },
]
