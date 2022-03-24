#Created by Petros Koutsouvelis in 03/2022

from os.path import dirname, join
from PIL import Image
import matplotlib
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import io
import base64

def pdr(sex, height):
    #Declare variables
    x_vals = []
    y_vals = []
    sex = int(sex)
    height = int(height)

    #Row indices for every 0.5 seconds (? to be changed), depending on the sampling rate
    #Assume sampling rate is 4 Hz
    sampling_rate = 50
    desired_rate = 2
    ratio = int(sampling_rate/desired_rate) #data to read every iteration

    #Set step length based on inputs
    if (sex):
        k = 0.415
    else:
        k = 0.413
        
    l = (k * height)/100 #to meters

    #Append for starting position
    x_vals.append(0)
    y_vals.append(0)
    prev_x = 0
    prev_y = 0
    gyro_angle = 0

    inputfile = 'Sensor_20220321_190524_8170278612129910134.csv'
    filename = join(dirname(__file__), inputfile)
    #inputfile = str(filename)
    inputfile_reader = pd.read_csv(filename) # Make each row a list with one string-type element

    #Convert vector acceleration to scalar
    acc_arr = []
    scalar_acc = 0

    for i in range(len(inputfile_reader)):
        scalar_acc = np.sqrt((inputfile_reader.iloc[i, 4]**2) + (inputfile_reader.iloc[i, 5]**2) +
                               (inputfile_reader.iloc[i, 4]**2))
        acc_arr.append(scalar_acc)
        
    #Remove gravity
    ave_acc = np.mean(acc_arr)
    for i in range(len(inputfile_reader)):
        acc_arr[i] = acc_arr[i] - ave_acc


    #Calculate and plot trajectory
    #-------------------------------------------------------------------

    #Perform smoothing as in paper "Fusion of WiFi, Smartphone Sensors
    #and Landmarks Using the Kalman Filter for Indoor Localization"

    m = 10 #smoothing factor
    acc_y = inputfile_reader.iloc[:, 5].values #vertical acceleration
    a_m = [] #acceleration after smoothing
    for i in range(len(inputfile_reader) - m):
        sum_m = 0
        for j in range(m):
            sum_m = sum_m + acc_arr[i + m]
        avg = sum_m/m
        a_m.append(avg)
        
    #Smoothing for gyroscope
    gyro_z = inputfile_reader.iloc[:, 9].values #vertical angular velocity
    g_m = [] #angular velocity after smoothing
    for i in range(len(inputfile_reader) - m):
        sum_m = 0
        for j in range(m):
            sum_m = sum_m + gyro_z[i + m]
        avg = sum_m/m
        g_m.append(avg)
        
    #Compute new coordinates
    dT = 1/desired_rate
    threshold = 2
    step_detect = 0
    new_x = 0
    new_y = 0
    for i in range(len(a_m) - ratio + 1):
        for j in a_m[i : (i + ratio)]:
            if (j > threshold):
                step_detect = 1
        if (step_detect):
            for j in range(ratio):
                gyro_angle = gyro_angle + (dT/ratio) * g_m[i + j]
            new_x = prev_x + l * np.sin(np.deg2rad(gyro_angle))
            new_y = prev_y + l * np.cos(np.deg2rad(gyro_angle))
            prev_x = new_x
            prev_y = new_y
            x_vals.append(new_x)
            y_vals.append(new_y)
            #print(gyro_angle)
            
        step_detect = 0

    #Plot trajectory
    fig = plt.figure()
    a1 = fig.add_subplot(1,1,1)
    a1.plot(x_vals, y_vals)
    #a1.set_title('PDR Trajectory')
    #a1.set_xlabel('Horizontal displacement (meters)')
    #a1.set_ylabel('Vertical displacement (meters)')

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

    













