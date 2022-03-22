#Created by Petros Koutsouvelis in 03/2022

from os.path import dirname, join
from PIL import Image
import matplotlib
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import io
import base64

def sensor_plot(sensor):
    #Declare variables
    sensor = int(sensor)
    sampling_rate = 4

    inputfile = 'Sensor_20220321_190524_8170278612129910134.csv'
    filename = join(dirname(__file__), inputfile)
    #inputfile = str(filename)
    inputfile_reader = pd.read_csv(filename) # Make each row a list with one string-type element
    
    measurement = ['Magnetometer measurement (uT): X-axis',
    'Magnetometer measurement (uT): Y-axis',
    'Magnetometer measurement (uT): Z-axis',
    'Magnetic field strength measurement (V/m)',
    'Accelerometer measurement (m/s2): X-axis',
    'Accelerometer measurement (m/s2): Y-axis',
    'Accelerometer measurement (m/s2): Z-axis',
    'Gyroscope measurement (deg/s): X-axis',
    'Gyroscope measurement (deg/s): Y-axis',
    'Gyroscope measurement (deg/s): Z-axis']
    
    #Scale x_axis to seconds
    x = range(1, (len(inputfile_reader) + 1))
    x_values = []
    for j in x:
        j = float(j/sampling_rate)
        x_values.append(j)
    
    #Generate plot
    fig = plt.figure()
    p1 = fig.add_subplot(1,1,1)
    p1.plot(x_values, inputfile_reader.iloc[:, sensor])
    #p1.set_title(measurement[sensor])
    #p1.set_xlabel('Time elapsed in seconds')
    #p1.set_ylabel('Amplitude')
    p1.grid()

    #Use this canvas to convert image to numpy array
    fig.canvas.draw()
    img = np.fromstring(fig.canvas.tostring_rgb(), dtype = np.uint8, sep = '')
    img = img.reshape(fig.canvas.get_width_height()[::-1] + (3,))
    #img = cv2.cvtColor(img, cv2.COLOR_RGB2BGR)

    #Convert to PIL image and then to byte string to return it in the java code
    pil_im = Image.fromarray(img)
    buff = io.BytesIO()
    pil_im.save(buff, format = 'PNG')
    img_str = base64.b64encode(buff.getvalue())
    out = "" + str(img_str, 'utf-8')
    
    return out


